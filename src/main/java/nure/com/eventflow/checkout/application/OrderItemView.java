package nure.com.eventflow.checkout.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemView(
        UUID ticketTypeId,
        String ticketName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String currency
) {
}
