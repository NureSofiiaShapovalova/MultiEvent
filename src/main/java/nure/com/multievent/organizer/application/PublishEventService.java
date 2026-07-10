package nure.com.multievent.organizer.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.domain.EventOwnershipPolicy;
import nure.com.multievent.organizer.domain.EventPublishAction;
import nure.com.multievent.organizer.domain.EventPublishHistory;
import nure.com.multievent.organizer.domain.EventPublishingPolicy;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.organizer.infra.EventPublishHistoryRepository;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import nure.com.multievent.notifications.domain.EventPublishedEvent;
import nure.com.multievent.shared.exception.NotFoundException;
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
