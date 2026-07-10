package nure.com.multievent.organizer.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.domain.Venue;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.catalog.infra.VenueRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEventService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public CreateEventService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            VenueRepository venueRepository,
            OrganizerEventRepository organizerEventRepository,
            AuditLogRepository auditLogRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Transactional
    public java.util.UUID create(String organizerEmail, CreateEventCommand command) {
        User organizer = findOrganizer(organizerEmail);
        ensureSlugIsFree(command.slug(), null);

        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new NotFoundException("Category was not found."));

        Venue venue = venueRepository.save(new Venue(
                command.venueName(),
                command.city(),
                command.addressLine(),
                command.postalCode(),
                command.country()));

        Event event = new Event(
                organizer,
                category,
                venue,
                command.title(),
                command.slug(),
                command.shortDescription(),
                command.fullDescription(),
                EventStatus.DRAFT,
                command.startsAt(),
                command.endsAt(),
                null);

        event.replaceImages(buildImages(event, command.imageUrls()));

        Event savedEvent = organizerEventRepository.save(event);
        auditLogRepository.save(new AuditLog(
                organizer,
                "Event",
                savedEvent.getId(),
                "CREATE_EVENT",
                "Created organizer event at " + clock.instant()));
        return savedEvent.getId();
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = EmailNormalizer.normalize(organizerEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private void ensureSlugIsFree(String slug, java.util.UUID currentEventId) {
        organizerEventRepository.findBySlug(slug).ifPresent(existing -> {
            if (currentEventId == null || !Objects.equals(existing.getId(), currentEventId)) {
                throw new BusinessRuleViolationException("An event with this slug already exists.");
            }
        });
    }

    private List<EventImage> buildImages(Event event, List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        List<String> normalized = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        List<EventImage> images = new java.util.ArrayList<>();
        for (int index = 0; index < normalized.size(); index++) {
            images.add(new EventImage(event, normalized.get(index), index == 0, index));
        }
        return images;
    }
}
