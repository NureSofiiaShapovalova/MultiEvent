package nure.com.multievent.checkout.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketCheckoutView(
        UUID ticketTypeId,
        String eventTitle,
        String eventSlug,
        String ticketName,
        String description,
        BigDecimal price,
        String currency,
        int maxPerOrder,
        int availableQuantity,
        String availabilityLabel
) {
}
