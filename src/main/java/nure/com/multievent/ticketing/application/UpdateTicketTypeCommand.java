package nure.com.multievent.ticketing.application;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateTicketTypeCommand(
        String name,
        String description,
        BigDecimal price,
        String currency,
        int maxPerOrder,
        int totalQuantity,
        Instant salesStart,
        Instant salesEnd
) {
}
