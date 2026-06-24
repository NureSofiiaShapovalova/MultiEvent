package nure.com.eventflow.organizer.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.VenueRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.domain.EventOwnershipPolicy;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateEventService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventOwnershipPolicy eventOwnershipPolicy;

    public UpdateEventService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            VenueRepository venueRepository,
            OrganizerEventRepository organizerEventRepository,
            AuditLogRepository auditLogRepository,
            EventOwnershipPolicy eventOwnershipPolicy
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventOwnershipPolicy = eventOwnershipPolicy;
    }

    @Transactional
    public void update(String organizerEmail, UUID eventId, UpdateEventCommand command) {
        User organizer = findOrganizer(organizerEmail);
        Event event = organizerEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        eventOwnershipPolicy.assertOwnedBy(organizer.getId(), event);
        ensureSlugIsFree(command.slug(), event.getId());

        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new NotFoundException("Category was not found."));
        Venue venue = event.getVenue();
        if (venue == null) {
            venue = venueRepository.save(new Venue(
                    command.venueName(),
                    command.city(),
                    command.addressLine(),
                    command.postalCode(),
                    command.country()));
        } else {
            venue.update(
                    command.venueName(),
                    command.city(),
                    command.addressLine(),
                    command.postalCode(),
                    command.country());
        }

        event.changeDetails(
                command.title(),
                command.slug(),
                command.shortDescription(),
                command.fullDescription(),
                command.startsAt(),
                command.endsAt());
        event.changeCategory(category);
        event.changeVenue(venue);
        event.replaceImages(buildImages(event, command.imageUrls()));

        organizerEventRepository.save(event);
        auditLogRepository.save(new AuditLog(
                organizer,
                "Event",
                event.getId(),
                "UPDATE_EVENT",
                "Updated organizer event."));
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = EmailNormalizer.normalize(organizerEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private void ensureSlugIsFree(String slug, UUID currentEventId) {
        organizerEventRepository.findBySlug(slug).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentEventId)) {
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
