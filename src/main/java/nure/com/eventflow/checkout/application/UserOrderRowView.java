package nure.com.eventflow.checkout.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserOrderRowView(
        UUID orderId,
        String eventTitle,
        String eventSlug,
        BigDecimal totalAmount,
        String currency,
        String status,
        Instant createdAt,
        Instant expiresAt,
        int itemCount,
        int totalQuantity
) {
}
