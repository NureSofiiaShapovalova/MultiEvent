package nure.com.eventflow.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class PaymentPolicy {

    public void assertCanStart(Order order, Instant now) {
        if (order == null) {
            throw new BusinessRuleViolationException("Order must exist.");
        }
        if (!order.isPendingPayment()) {
            throw new BusinessRuleViolationException("Order is no longer pending payment.");
        }
        if (!now.isBefore(order.getExpiresAt())) {
            throw new BusinessRuleViolationException("Order has expired.");
        }
    }

    public void assertAmountsMatch(Order order, BigDecimal amount, String currency) {
        if (order == null) {
            throw new BusinessRuleViolationException("Order must exist.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleViolationException("Payment amount must be positive.");
        }
        if (currency == null || currency.isBlank() || currency.trim().length() != 3) {
            throw new BusinessRuleViolationException("Currency must be a 3-letter code.");
        }
        if (amount.compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessRuleViolationException("Payment amount must match the order total.");
        }
        if (!order.getCurrency().equalsIgnoreCase(currency.trim())) {
            throw new BusinessRuleViolationException("Payment currency must match the order currency.");
        }
    }

    public void assertCanConfirm(Order order, Payment payment, Instant now) {
        if (payment == null) {
            throw new BusinessRuleViolationException("Payment must exist.");
        }
        if (payment.isPaid()) {
            return;
        }
        if (!payment.isPending()) {
            throw new BusinessRuleViolationException("Payment cannot be confirmed in its current state.");
        }
        assertCanStart(order, now);
        assertAmountsMatch(order, payment.getAmount(), payment.getCurrency());
    }

    public void assertCanRetry(Order order, Payment payment, Instant now) {
        if (payment == null) {
            throw new BusinessRuleViolationException("Payment must exist.");
        }
        if (!payment.isFailed()) {
            throw new BusinessRuleViolationException("Payment can only be retried after a failure.");
        }
        assertCanStart(order, now);
        assertAmountsMatch(order, payment.getAmount(), payment.getCurrency());
    }
}
