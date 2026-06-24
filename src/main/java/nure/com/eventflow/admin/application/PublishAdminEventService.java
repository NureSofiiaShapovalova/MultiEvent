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
import nure.com.eventflow.notifications.domain.EventPublishedEvent;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.domain.EventPublishAction;
import nure.com.eventflow.organizer.domain.EventPublishHistory;
import nure.com.eventflow.organizer.domain.EventPublishingPolicy;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.EventPublishHistoryRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishAdminEventService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final EventPublishHistoryRepository eventPublishHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventPublishingPolicy eventPublishingPolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public PublishAdminEventService(
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
    public void publish(String adminEmail, UUID eventId) {
        User admin = findAdmin(adminEmail);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        Instant publishedAt = clock.instant();
        eventPublishingPolicy.markPublished(event, publishedAt);
        eventRepository.save(event);
        eventPublishHistoryRepository.save(new EventPublishHistory(
                event,
                admin,
                EventPublishAction.PUBLISHED,
                "Published by admin at " + publishedAt));
        moderationActionRepository.save(new ModerationAction(
                admin,
                event,
                ModerationActionType.PUBLISH,
                "Published by admin at " + publishedAt));
        auditLogRepository.save(new AuditLog(
                admin,
                "Event",
                event.getId(),
                "ADMIN_PUBLISH_EVENT",
                "Published event at " + publishedAt));
        applicationEventPublisher.publishEvent(new EventPublishedEvent(event.getId(), event.getOrganizer().getId()));
    }

    private User findAdmin(String adminEmail) {
        String normalizedEmail = EmailNormalizer.normalize(adminEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
