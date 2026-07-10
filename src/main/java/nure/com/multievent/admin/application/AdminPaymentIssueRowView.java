package nure.com.multievent.admin.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.checkout.domain.OrderStatus;
import nure.com.multievent.payment.domain.PaymentStatus;

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
