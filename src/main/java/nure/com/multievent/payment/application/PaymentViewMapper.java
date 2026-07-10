package nure.com.multievent.payment.application;

import java.time.Instant;
import nure.com.multievent.checkout.application.OrderItemView;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentViewMapper {

    public PaymentPageView toView(Payment payment, Instant now) {
        var order = payment.getOrder();
        var items = order.getItems().stream()
                .map(item -> new OrderItemView(
                        item.getTicketType().getId(),
                        item.getTicketName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal(),
                        item.getCurrency()))
                .toList();

        boolean canConfirm = payment.isPending() && order.isPendingPayment() && now.isBefore(order.getExpiresAt());
        boolean canRetry = payment.isFailed() && order.isPendingPayment() && now.isBefore(order.getExpiresAt());

        return new PaymentPageView(
                order.getId(),
                payment.getId(),
                order.getEvent().getTitle(),
                order.getEvent().getSlug(),
                items,
                payment.getAmount(),
                payment.getCurrency(),
                order.getStatus().name(),
                payment.getStatus().name(),
                payment.getProviderName(),
                payment.getProviderReference(),
                payment.getProviderApprovalUrl(),
                payment.getStartedAt(),
                payment.getCompletedAt(),
                order.getExpiresAt(),
                payment.getFailureReason(),
                payment.getAttempts().size(),
                message(payment, order),
                canConfirm,
                canRetry);
    }

    private String message(Payment payment, nure.com.multievent.checkout.domain.Order order) {
        if (!order.isPendingPayment() && !payment.isPaid()) {
            return "This order is no longer available for payment.";
        }
        if (payment.isPaid()) {
            return "Payment completed.";
        }
        if (payment.isFailed()) {
            return payment.getFailureReason() != null ? payment.getFailureReason() : "Payment failed.";
        }
        return "Review the order and complete payment when ready.";
    }
}
