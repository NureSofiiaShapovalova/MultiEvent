package nure.com.eventflow.ticketing.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateTicketTypeCommand(
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
