package nure.com.multievent.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderItem;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void tracksStatusTransitionsAndAttemptNumbers() {
        Order order = order();
        Payment payment = new Payment(
                order.getCustomer(),
                order,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                Instant.parse("2026-08-15T00:00:00Z"));

        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.PENDING,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                null));
        payment.markFailed("Gateway timeout", Instant.parse("2026-08-15T00:01:00Z"));
        payment.retry("LOCAL_MANUAL", "local-ref-2", Instant.parse("2026-08-15T00:02:00Z"));
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.RETRY,
                PaymentStatus.PENDING,
                "LOCAL_MANUAL",
                "local-ref-2",
                order.getTotalAmount(),
                order.getCurrency(),
                null));
        payment.markPaid(Instant.parse("2026-08-15T00:03:00Z"));

        assertThat(payment.isPaid()).isTrue();
        assertThat(payment.getAttempts()).hasSize(2);
        assertThat(payment.getAttempts().get(0).getAttemptNumber()).isEqualTo(1);
        assertThat(payment.getAttempts().get(1).getAttemptNumber()).isEqualTo(2);
        assertThat(payment.getCompletedAt()).isEqualTo(Instant.parse("2026-08-15T00:03:00Z"));
    }

    @Test
    void rejectsInvalidConstructorAndTransitions() {
        Order order = order();

        assertThatThrownBy(() -> new Payment(
                order.getCustomer(),
                order,
                "LOCAL_MANUAL",
                "local-ref",
                BigDecimal.ZERO,
                order.getCurrency(),
                Instant.parse("2026-08-15T00:00:00Z")))
                .hasMessage("Payment amount must be positive.");

        Payment payment = new Payment(
                order.getCustomer(),
                order,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                Instant.parse("2026-08-15T00:00:00Z"));
        payment.markPaid(Instant.parse("2026-08-15T00:01:00Z"));

        assertThatThrownBy(() -> payment.markPaid(Instant.parse("2026-08-15T00:01:00Z")))
                .hasMessage("Payment is no longer pending.");
        assertThatThrownBy(() -> payment.retry("LOCAL_MANUAL", "local-ref-2", Instant.parse("2026-08-15T00:02:00Z")))
                .hasMessage("Payment can only be retried after a failure.");
    }

    private Order order() {
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
        Order order = new Order(customer, event, Instant.parse("2026-08-15T00:15:00Z"));
        order.addItem(new OrderItem(ticketType, 2));
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        TestEntityFactory.setId(order, java.util.UUID.randomUUID());
        return order;
    }
}
