package nure.com.eventflow.ticketing.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrganizerTicketRowView(
        UUID id,
        UUID eventId,
        String eventTitle,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int maxPerOrder,
        Instant salesStart,
        Instant salesEnd,
        boolean active,
        int totalQuantity,
        int reservedQuantity,
        int soldQuantity,
        int availableQuantity,
        String availabilityLabel
) {
}
