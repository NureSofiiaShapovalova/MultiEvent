package nure.com.eventflow.ticketing.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketOptionView(
        UUID ticketTypeId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int maxPerOrder,
        int availableQuantity,
        boolean purchasable,
        String availabilityLabel
) {
}
