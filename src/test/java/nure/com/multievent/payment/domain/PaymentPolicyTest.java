package nure.com.multievent.payment.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.payment.domain.PaymentAttemptType;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;

class PaymentPolicyTest {

    private final PaymentPolicy paymentPolicy = new PaymentPolicy();

    @Test
    void allowsStartingConfirmationAndRetryForValidState() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        Order order = order(now.plusSeconds(900));
        Payment payment = payment(order, PaymentStatus.PENDING);

        paymentPolicy.assertCanStart(order, now);
        paymentPolicy.assertAmountsMatch(order, order.getTotalAmount(), order.getCurrency());
        paymentPolicy.assertCanConfirm(order, payment, now);

        payment.markFailed("Gateway timeout", now.plusSeconds(5));
        paymentPolicy.assertCanRetry(order, payment, now.plusSeconds(5));
    }

    @Test
    void rejectsWrongStateAndMismatchedAmount() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        Order expiredOrder = order(now.minusSeconds(1));
        Payment pendingPayment = payment(order(now.plusSeconds(900)), PaymentStatus.PENDING);

        assertThatThrownBy(() -> paymentPolicy.assertCanStart(expiredOrder, now))
                .hasMessage("Order has expired.");
        assertThatThrownBy(() -> paymentPolicy.assertAmountsMatch(pendingPayment.getOrder(), BigDecimal.valueOf(49), "USD"))
                .hasMessage("Payment amount must match the order total.");
        assertThatThrownBy(() -> paymentPolicy.assertAmountsMatch(pendingPayment.getOrder(), BigDecimal.valueOf(50), "EUR"))
                .hasMessage("Payment currency must match the order currency.");
    }

    @Test
    void rejectsInvalidConfirmAndRetryStates() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        Order order = order(now.plusSeconds(900));
        Payment failedPayment = payment(order, PaymentStatus.FAILED);
        Payment paidPayment = payment(order, PaymentStatus.PAID);

        assertThatThrownBy(() -> paymentPolicy.assertCanConfirm(order, failedPayment, now))
                .hasMessage("Payment cannot be confirmed in its current state.");
        assertThatThrownBy(() -> paymentPolicy.assertCanRetry(order, paidPayment, now))
                .hasMessage("Payment can only be retried after a failure.");
    }

    private Order order(Instant expiresAt) {
        User customer = TestEntityFactory.user("buyer@example.com");
        Event event = TestEntityFactory.event(
                TestEntityFactory.user("organizer@example.com"),
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.PUBLISHED);
        var ticketType = TestEntityFactory.ticketType(
                event,
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
        Order order = new Order(customer, event, expiresAt);
        order.addItem(new nure.com.multievent.checkout.domain.OrderItem(ticketType, 2));
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        TestEntityFactory.setId(order, java.util.UUID.randomUUID());
        return order;
    }

    private Payment payment(Order order, PaymentStatus status) {
        Payment payment = new Payment(
                order.getCustomer(),
                order,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                Instant.parse("2026-08-15T00:00:00Z"));
        TestEntityFactory.setId(payment, java.util.UUID.randomUUID());
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.PENDING,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                null));
        if (status == PaymentStatus.PAID) {
            payment.markPaid(Instant.parse("2026-08-15T00:05:00Z"));
        } else if (status == PaymentStatus.FAILED) {
            payment.markFailed("Gateway timeout", Instant.parse("2026-08-15T00:05:00Z"));
        }
        return payment;
    }
}
