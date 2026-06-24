package nure.com.eventflow.organizer.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.catalog.application.CategoryOptionView;
import nure.com.eventflow.catalog.domain.EventStatus;

public record EventEditorView(
        UUID eventId,
        String title,
        String slug,
        String shortDescription,
        String fullDescription,
        UUID categoryId,
        String venueName,
        String city,
        String addressLine,
        String postalCode,
        String country,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<String> imageUrls,
        EventStatus status,
        List<CategoryOptionView> categories
) {
}
