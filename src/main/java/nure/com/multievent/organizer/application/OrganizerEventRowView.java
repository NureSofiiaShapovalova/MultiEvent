package nure.com.multievent.organizer.application;

import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.catalog.domain.EventStatus;

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
