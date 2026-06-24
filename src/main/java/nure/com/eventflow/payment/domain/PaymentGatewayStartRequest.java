package nure.com.eventflow.payment.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentGatewayStartRequest(
        UUID orderId,
        BigDecimal amount,
        String currency
) {
}
