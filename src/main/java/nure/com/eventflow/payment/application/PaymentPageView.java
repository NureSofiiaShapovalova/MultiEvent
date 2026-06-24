package nure.com.eventflow.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.checkout.application.OrderItemView;

public record PaymentPageView(
        UUID orderId,
        UUID paymentId,
        String eventTitle,
        String eventSlug,
        List<OrderItemView> items,
        BigDecimal totalAmount,
        String currency,
        String orderStatus,
        String paymentStatus,
        String providerName,
        String providerReference,
        String providerApprovalUrl,
        Instant startedAt,
        Instant completedAt,
        Instant expiresAt,
        String failureReason,
        int attemptCount,
        String message,
        boolean canConfirm,
        boolean canRetry
) {

    public PaymentPageView(
            UUID orderId,
            UUID paymentId,
            String eventTitle,
            String eventSlug,
            List<OrderItemView> items,
            BigDecimal totalAmount,
            String currency,
            String orderStatus,
            String paymentStatus,
            String providerName,
            String providerReference,
            Instant startedAt,
            Instant completedAt,
            Instant expiresAt,
            String failureReason,
            int attemptCount,
            String message,
            boolean canConfirm,
            boolean canRetry
    ) {
        this(
                orderId,
                paymentId,
                eventTitle,
                eventSlug,
                items,
                totalAmount,
                currency,
                orderStatus,
                paymentStatus,
                providerName,
                providerReference,
                null,
                startedAt,
                completedAt,
                expiresAt,
                failureReason,
                attemptCount,
                message,
                canConfirm,
                canRetry);
    }
}
