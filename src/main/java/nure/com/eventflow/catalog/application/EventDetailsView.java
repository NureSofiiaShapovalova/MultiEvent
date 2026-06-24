package nure.com.eventflow.catalog.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.ticketing.application.TicketOptionView;

public record EventDetailsView(
        UUID id,
        String title,
        String slug,
        String shortDescription,
        String fullDescription,
        String categoryName,
        String venueName,
        String venueCity,
        String venueAddressLine,
        String venuePostalCode,
        String venueCountry,
        Instant startsAt,
        Instant endsAt,
        Instant publishedAt,
        String organizerEmail,
        String coverImageUrl,
        List<String> imageUrls,
        List<TicketOptionView> ticketOptions
) {
}
