package nure.com.eventflow.admin.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.admin.domain.ModerationAction;
import nure.com.eventflow.admin.domain.ModerationActionType;
import nure.com.eventflow.admin.infra.ModerationActionRepository;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.EventCancelledEvent;
import nure.com.eventflow.notifications.domain.EventPublishedEvent;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.domain.EventPublishAction;
import nure.com.eventflow.organizer.domain.EventPublishHistory;
import nure.com.eventflow.organizer.domain.EventPublishingPolicy;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.EventPublishHistoryRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerateEventService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final EventPublishHistoryRepository eventPublishHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventPublishingPolicy eventPublishingPolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public ModerateEventService(
            UserRepository userRepository,
            EventRepository eventRepository,
            ModerationActionRepository moderationActionRepository,
            EventPublishHistoryRepository eventPublishHistoryRepository,
            AuditLogRepository auditLogRepository,
            EventPublishingPolicy eventPublishingPolicy,
            ApplicationEventPublisher applicationEventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.eventPublishHistoryRepository = eventPublishHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventPublishingPolicy = eventPublishingPolicy;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void moderate(String adminEmail, UUID eventId, ModerationCommand command) {
        User admin = findAdmin(adminEmail);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        ModerationActionType action = requireAction(command);
        String reason = command == null ? null : normalizeReason(command.reason());
        Instant moderatedAt = clock.instant();

        if (action == ModerationActionType.PUBLISH) {
            eventPublishingPolicy.markPublished(event, moderatedAt);
            eventPublishHistoryRepository.save(new EventPublishHistory(
                    event,
                    admin,
                    EventPublishAction.PUBLISHED,
                    detail(reason, "Published by admin at " + moderatedAt)));
            applicationEventPublisher.publishEvent(new EventPublishedEvent(event.getId(), event.getOrganizer().getId()));
        } else if (action == ModerationActionType.CANCEL) {
            eventPublishingPolicy.markCancelled(event, moderatedAt);
            eventPublishHistoryRepository.save(new EventPublishHistory(
                    event,
                    admin,
                    EventPublishAction.CANCELLED,
                    detail(reason, "Cancelled by admin at " + moderatedAt)));
            applicationEventPublisher.publishEvent(new EventCancelledEvent(event.getId(), event.getOrganizer().getId()));
        } else {
            throw new BusinessRuleViolationException("Unsupported moderation action.");
        }

        eventRepository.save(event);
        moderationActionRepository.save(new ModerationAction(admin, event, action, reason));
        auditLogRepository.save(new AuditLog(
                admin,
                "Event",
                event.getId(),
                "MODERATE_EVENT_" + action.name(),
                detail(reason, "Moderated event at " + moderatedAt)));
    }

    private User findAdmin(String adminEmail) {
        String normalizedEmail = EmailNormalizer.normalize(adminEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private static ModerationActionType requireAction(ModerationCommand command) {
        if (command == null || command.action() == null) {
            throw new BusinessRuleViolationException("Moderation action is required.");
        }
        return command.action();
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private static String detail(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason;
    }
}
