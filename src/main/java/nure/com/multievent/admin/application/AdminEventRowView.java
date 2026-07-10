package nure.com.multievent.admin.application;

import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.catalog.domain.EventStatus;

public record AdminEventRowView(
        UUID id,
        String title,
        String slug,
        EventStatus status,
        String categoryName,
        String organizerEmail,
        String city,
        Instant startsAt,
        Instant endsAt,
        Instant publishedAt,
        String coverImageUrl
) {
}
