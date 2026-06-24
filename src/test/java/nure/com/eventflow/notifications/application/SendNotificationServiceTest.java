package nure.com.eventflow.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.notifications.domain.EventSubscriptionPreference;
import nure.com.eventflow.notifications.domain.NotificationRecord;
import nure.com.eventflow.notifications.domain.NotificationStatus;
import nure.com.eventflow.notifications.domain.NotificationTemplateCode;
import nure.com.eventflow.notifications.infra.MailNotificationSender;
import nure.com.eventflow.notifications.infra.NotificationLogRepository;
import nure.com.eventflow.notifications.infra.SubscriptionPreferenceRepository;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SendNotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private SubscriptionPreferenceRepository subscriptionPreferenceRepository;

    @Mock
    private MailNotificationSender mailNotificationSender;

    @Mock
    private Clock clock;

    private SendNotificationService sendNotificationService;

    @BeforeEach
    void setUp() {
        sendNotificationService = new SendNotificationService(
                notificationLogRepository,
                subscriptionPreferenceRepository,
                new NotificationTemplateRenderer(),
                mailNotificationSender,
                new ObjectMapper(),
                clock);
    }

    @Test
    void sendsNotificationAndMarksItSentWhenMailDeliverySucceeds() {
        Instant now = Instant.parse("2026-06-22T12:00:00Z");
        User recipient = TestEntityFactory.user("buyer@example.com");
        when(clock.instant()).thenReturn(now);
        when(subscriptionPreferenceRepository.findByUser_Id(recipient.getId()))
                .thenReturn(Optional.of(new EventSubscriptionPreference(recipient, true, true, false)));
        when(notificationLogRepository.save(any(NotificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRecord record = sendNotificationService.send(
                recipient,
                NotificationTemplateCode.WELCOME,
                Map.of("email", recipient.getEmail()));

        assertThat(record.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(record.getSentAt()).isEqualTo(now);
        assertThat(record.getPayloadJson()).contains("\"email\":\"buyer@example.com\"");

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailNotificationSender).send(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());
        assertThat(toCaptor.getValue()).isEqualTo("buyer@example.com");
        assertThat(subjectCaptor.getValue()).contains("Welcome");
        assertThat(bodyCaptor.getValue()).contains("buyer@example.com");
    }

    @Test
    void marksNotificationFailedWhenPreferenceGateBlocksDelivery() {
        User recipient = TestEntityFactory.user("buyer@example.com");
        when(subscriptionPreferenceRepository.findByUser_Id(recipient.getId()))
                .thenReturn(Optional.of(new EventSubscriptionPreference(recipient, false, true, false)));
        when(notificationLogRepository.save(any(NotificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRecord record = sendNotificationService.send(
                recipient,
                NotificationTemplateCode.WELCOME,
                Map.of("email", recipient.getEmail()));

        assertThat(record.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(record.getFailureReason()).isEqualTo("Email notifications are disabled.");
        verify(mailNotificationSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void marksNotificationFailedWhenMailSenderThrows() {
        User recipient = TestEntityFactory.user("buyer@example.com");
        when(subscriptionPreferenceRepository.findByUser_Id(recipient.getId()))
                .thenReturn(Optional.of(new EventSubscriptionPreference(recipient, true, true, false)));
        when(notificationLogRepository.save(any(NotificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailSendException("SMTP down"))
                .when(mailNotificationSender)
                .send(anyString(), anyString(), anyString());

        NotificationRecord record = sendNotificationService.send(
                recipient,
                NotificationTemplateCode.WELCOME,
                Map.of("email", recipient.getEmail()));

        assertThat(record.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(record.getFailureReason()).contains("SMTP down");
    }
}
