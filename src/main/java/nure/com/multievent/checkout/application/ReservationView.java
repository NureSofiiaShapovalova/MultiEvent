package nure.com.multievent.checkout.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationView(
        UUID reservationId,
        String eventTitle,
        String eventSlug,
        UUID ticketTypeId,
        String ticketName,
        String description,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal totalPrice,
        int availableQuantity,
        Instant expiresAt,
        String status
) {
}
