package nure.com.multievent.notifications.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.notifications.domain.EventSubscriptionPreference;
import nure.com.multievent.notifications.domain.NotificationChannel;
import nure.com.multievent.notifications.domain.NotificationRecord;
import nure.com.multievent.notifications.domain.NotificationStatus;
import nure.com.multievent.notifications.domain.NotificationTemplateCode;
import nure.com.multievent.notifications.infra.MailNotificationSender;
import nure.com.multievent.notifications.infra.NotificationLogRepository;
import nure.com.multievent.notifications.infra.SubscriptionPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final SubscriptionPreferenceRepository subscriptionPreferenceRepository;
    private final NotificationTemplateRenderer notificationTemplateRenderer;
    private final MailNotificationSender mailNotificationSender;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SendNotificationService(
            NotificationLogRepository notificationLogRepository,
            SubscriptionPreferenceRepository subscriptionPreferenceRepository,
            NotificationTemplateRenderer notificationTemplateRenderer,
            MailNotificationSender mailNotificationSender,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.subscriptionPreferenceRepository = subscriptionPreferenceRepository;
        this.notificationTemplateRenderer = notificationTemplateRenderer;
        this.mailNotificationSender = mailNotificationSender;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationRecord send(User recipient, NotificationTemplateCode templateCode, Map<String, ?> payload) {
        EventSubscriptionPreference preferences = ensurePreferences(recipient);
        String payloadJson = serialize(payload);
        NotificationRecord record = notificationLogRepository.save(new NotificationRecord(
                recipient,
                NotificationChannel.EMAIL,
                templateCode,
                payloadJson));

        if (!isEnabled(templateCode, preferences)) {
            record.markFailed(failureMessage(templateCode));
            return notificationLogRepository.save(record);
        }

        try {
            RenderedNotification renderedNotification = notificationTemplateRenderer.render(templateCode, payload);
            mailNotificationSender.send(recipient.getEmail(), renderedNotification.subject(), renderedNotification.body());
            record.markSent(Instant.now(clock));
        } catch (RuntimeException exception) {
            record.markFailed(failureReason(exception));
        }
        return notificationLogRepository.save(record);
    }

    private EventSubscriptionPreference ensurePreferences(User recipient) {
        return subscriptionPreferenceRepository.findByUser_Id(recipient.getId())
                .orElseGet(() -> subscriptionPreferenceRepository.save(new EventSubscriptionPreference(recipient)));
    }

    private boolean isEnabled(NotificationTemplateCode templateCode, EventSubscriptionPreference preferences) {
        if (templateCode.requiresEmailEnabled() && !preferences.isEmailEnabled()) {
            return false;
        }
        if (templateCode.requiresOrderUpdatesEnabled() && !preferences.isOrderUpdatesEnabled()) {
            return false;
        }
        return !templateCode.requiresMarketingEnabled() || preferences.isMarketingEnabled();
    }

    private String failureMessage(NotificationTemplateCode templateCode) {
        if (templateCode.requiresMarketingEnabled()) {
            return "Marketing notifications are disabled.";
        }
        if (templateCode.requiresOrderUpdatesEnabled()) {
            return "Order update notifications are disabled.";
        }
        return "Email notifications are disabled.";
    }

    private String failureReason(RuntimeException exception) {
        Throwable mostSpecificCause = exception.getCause();
        String message = mostSpecificCause != null && mostSpecificCause.getMessage() != null
                ? mostSpecificCause.getMessage()
                : exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private String serialize(Map<String, ?> payload) {
        try {
            Map<String, Object> ordered = new LinkedHashMap<>(payload);
            return objectMapper.writeValueAsString(ordered);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize notification payload.", exception);
        }
    }
}
