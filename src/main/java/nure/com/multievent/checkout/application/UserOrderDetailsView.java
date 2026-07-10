package nure.com.multievent.checkout.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserOrderDetailsView(
        UUID orderId,
        String eventTitle,
        String eventSlug,
        List<OrderItemView> items,
        BigDecimal totalAmount,
        String currency,
        Instant expiresAt,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
