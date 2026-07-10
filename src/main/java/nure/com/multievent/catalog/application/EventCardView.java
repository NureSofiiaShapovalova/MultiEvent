package nure.com.multievent.catalog.application;

import java.time.Instant;
import java.util.UUID;

public record EventCardView(
        UUID id,
        String title,
        String slug,
        String shortDescription,
        String categoryName,
        String venueCity,
        Instant startsAt,
        Instant endsAt,
        String coverImageUrl
) {
}
