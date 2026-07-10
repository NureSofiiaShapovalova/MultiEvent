package nure.com.multievent.organizer.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import nure.com.multievent.catalog.application.CategoryOptionView;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.Venue;
import org.springframework.stereotype.Component;

@Component
public class OrganizerViewMapper {

    public OrganizerEventRowView toRowView(Event event) {
        return new OrganizerEventRowView(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getStatus(),
                event.getCategory().getName(),
                event.getVenue().getCity(),
                event.getStartsAt(),
                event.getEndsAt(),
                coverImageUrl(event));
    }

    public OrganizerDashboardView toDashboardView(
            String organizerEmail,
            long totalEvents,
            long draftEvents,
            long publishedEvents,
            long cancelledEvents,
            List<Event> recentEvents
    ) {
        return new OrganizerDashboardView(
                organizerEmail,
                totalEvents,
                draftEvents,
                publishedEvents,
                cancelledEvents,
                recentEvents.stream().map(this::toRowView).toList());
    }

    public EventEditorView toEditorView(Event event, List<CategoryOptionView> categories) {
        return new EventEditorView(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getShortDescription(),
                event.getFullDescription(),
                event.getCategory().getId(),
                event.getVenue().getName(),
                event.getVenue().getCity(),
                event.getVenue().getAddressLine(),
                event.getVenue().getPostalCode(),
                event.getVenue().getCountry(),
                LocalDateTime.ofInstant(event.getStartsAt(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(event.getEndsAt(), ZoneOffset.UTC),
                event.getImages().stream()
                        .sorted(Comparator.comparingInt(EventImage::getSortOrder))
                        .map(EventImage::getImageUrl)
                        .toList(),
                event.getStatus(),
                categories);
    }

    public EventEditorView toEmptyEditorView(List<CategoryOptionView> categories) {
        return new EventEditorView(
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                null,
                null,
                List.of(),
                null,
                categories);
    }

    public CreateEventCommand toCreateCommand(OrganizerEventForm form) {
        return new CreateEventCommand(
                form.getTitle(),
                form.getSlug(),
                form.getShortDescription(),
                form.getFullDescription(),
                form.getCategoryId(),
                form.getVenueName(),
                form.getCity(),
                form.getAddressLine(),
                form.getPostalCode(),
                form.getCountry(),
                form.getStartsAt().atZone(ZoneOffset.UTC).toInstant(),
                form.getEndsAt().atZone(ZoneOffset.UTC).toInstant(),
                form.getImageUrls());
    }

    public UpdateEventCommand toUpdateCommand(OrganizerEventForm form) {
        return new UpdateEventCommand(
                form.getTitle(),
                form.getSlug(),
                form.getShortDescription(),
                form.getFullDescription(),
                form.getCategoryId(),
                form.getVenueName(),
                form.getCity(),
                form.getAddressLine(),
                form.getPostalCode(),
                form.getCountry(),
                form.getStartsAt().atZone(ZoneOffset.UTC).toInstant(),
                form.getEndsAt().atZone(ZoneOffset.UTC).toInstant(),
                form.getImageUrls());
    }

    public OrganizerEventForm toForm(EventEditorView view) {
        OrganizerEventForm form = new OrganizerEventForm();
        form.setTitle(view.title());
        form.setSlug(view.slug());
        form.setShortDescription(view.shortDescription());
        form.setFullDescription(view.fullDescription());
        form.setCategoryId(view.categoryId());
        form.setVenueName(view.venueName());
        form.setCity(view.city());
        form.setAddressLine(view.addressLine());
        form.setPostalCode(view.postalCode());
        form.setCountry(view.country());
        form.setStartsAt(view.startsAt());
        form.setEndsAt(view.endsAt());
        form.setImageUrlsText(String.join(System.lineSeparator(), view.imageUrls()));
        return form;
    }

    public List<CategoryOptionView> toCategoryOptions(List<Category> categories) {
        return categories.stream()
                .map(category -> new CategoryOptionView(category.getId(), category.getName(), category.getSlug()))
                .toList();
    }

    public CancelEventCommand toCancelCommand(OrganizerCancelForm form) {
        return new CancelEventCommand(form.getNotes());
    }

    private static String coverImageUrl(Event event) {
        return event.getImages().stream()
                .sorted(Comparator.comparingInt(EventImage::getSortOrder))
                .filter(EventImage::isCover)
                .map(EventImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> event.getImages().stream()
                        .sorted(Comparator.comparingInt(EventImage::getSortOrder))
                        .map(EventImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
    }
}
