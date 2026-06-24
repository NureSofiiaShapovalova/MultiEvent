package nure.com.eventflow.admin.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.checkout.domain.OrderStatus;
import nure.com.eventflow.payment.domain.PaymentStatus;

public record AdminPaymentIssueRowView(
        UUID paymentId,
        UUID orderId,
        String eventTitle,
        String customerEmail,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        BigDecimal amount,
        String currency,
        String providerName,
        String providerReference,
        Instant startedAt,
        Instant completedAt,
        String failureReason,
        int attemptCount
) {
}
