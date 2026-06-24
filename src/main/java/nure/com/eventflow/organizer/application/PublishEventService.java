package nure.com.eventflow.organizer.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.domain.EventOwnershipPolicy;
import nure.com.eventflow.organizer.domain.EventPublishAction;
import nure.com.eventflow.organizer.domain.EventPublishHistory;
import nure.com.eventflow.organizer.domain.EventPublishingPolicy;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.EventPublishHistoryRepository;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import nure.com.eventflow.notifications.domain.EventPublishedEvent;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishEventService {

    private final UserRepository userRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final EventPublishHistoryRepository eventPublishHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventOwnershipPolicy eventOwnershipPolicy;
    private final EventPublishingPolicy eventPublishingPolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public PublishEventService(
            UserRepository userRepository,
            OrganizerEventRepository organizerEventRepository,
            EventPublishHistoryRepository eventPublishHistoryRepository,
            AuditLogRepository auditLogRepository,
            EventOwnershipPolicy eventOwnershipPolicy,
            EventPublishingPolicy eventPublishingPolicy,
            ApplicationEventPublisher applicationEventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.eventPublishHistoryRepository = eventPublishHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventOwnershipPolicy = eventOwnershipPolicy;
        this.eventPublishingPolicy = eventPublishingPolicy;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void publish(String organizerEmail, UUID eventId) {
        User organizer = findOrganizer(organizerEmail);
        Event event = organizerEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        eventOwnershipPolicy.assertOwnedBy(organizer.getId(), event);
        Instant publishedAt = clock.instant();
        eventPublishingPolicy.markPublished(event, publishedAt);

        organizerEventRepository.save(event);
        eventPublishHistoryRepository.save(new EventPublishHistory(
                event,
                organizer,
                EventPublishAction.PUBLISHED,
                "Published at " + publishedAt));
        auditLogRepository.save(new AuditLog(
                organizer,
                "Event",
                event.getId(),
                "PUBLISH_EVENT",
                "Published organizer event at " + publishedAt));
        applicationEventPublisher.publishEvent(new EventPublishedEvent(event.getId(), organizer.getId()));
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = EmailNormalizer.normalize(organizerEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
