package nure.com.eventflow.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void marksOrderExpiredAndRejectsFurtherTransitions() {
        Order order = pendingOrder();

        order.markExpired();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThatThrownBy(order::cancel)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Order is no longer pending payment.");
    }

    @Test
    void marksOrderPaidAndRejectsFurtherTransitions() {
        Order order = pendingOrder();

        order.markPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThatThrownBy(order::markExpired)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Order is no longer pending payment.");
    }

    private Order pendingOrder() {
        TicketType ticketType = ticketType();
        Order order = new Order(
                TestEntityFactory.user("buyer@example.com"),
                ticketType.getEvent(),
                Instant.parse("2026-08-15T00:15:00Z"));
        order.addItem(new OrderItem(ticketType, 2));
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        return order;
    }

    private TicketType ticketType() {
        return TestEntityFactory.ticketType(
                TestEntityFactory.event(
                        TestEntityFactory.user("organizer@example.com"),
                        TestEntityFactory.category("Concerts", "concerts"),
                        TestEntityFactory.venue("Palace", "Kyiv"),
                        "rock-night",
                        nure.com.eventflow.catalog.domain.EventStatus.PUBLISHED),
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
