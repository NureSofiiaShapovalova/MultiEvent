package nure.com.eventflow.organizer.application;

import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.EventStatus;

public record OrganizerEventRowView(
        UUID id,
        String title,
        String slug,
        EventStatus status,
        String categoryName,
        String city,
        Instant startsAt,
        Instant endsAt,
        String coverImageUrl
) {
}
