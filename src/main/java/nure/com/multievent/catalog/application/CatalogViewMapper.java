package nure.com.multievent.catalog.application;

import java.util.Comparator;
import java.util.List;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.ticketing.application.TicketOptionView;
import org.springframework.stereotype.Component;

@Component
public class CatalogViewMapper {

    public EventCardView toCardView(Event event) {
        return new EventCardView(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getShortDescription(),
                event.getCategory().getName(),
                event.getVenue().getCity(),
                event.getStartsAt(),
                event.getEndsAt(),
                coverImageUrl(event));
    }

    public EventDetailsView toDetailsView(Event event, List<TicketOptionView> ticketOptions) {
        List<String> imageUrls = event.getImages().stream()
                .sorted(Comparator.comparingInt(EventImage::getSortOrder))
                .map(EventImage::getImageUrl)
                .toList();
        return new EventDetailsView(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getShortDescription(),
                event.getFullDescription(),
                event.getCategory().getName(),
                event.getVenue().getName(),
                event.getVenue().getCity(),
                event.getVenue().getAddressLine(),
                event.getVenue().getPostalCode(),
                event.getVenue().getCountry(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getPublishedAt(),
                event.getOrganizer().getEmail(),
                coverImageUrl(event),
                imageUrls,
                ticketOptions);
    }

    public CategoryOptionView toCategoryOptionView(Category category) {
        return new CategoryOptionView(category.getId(), category.getName(), category.getSlug());
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
