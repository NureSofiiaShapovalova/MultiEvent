package nure.com.multievent.checkout.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.ticketing.domain.TicketSalesPolicy;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;

class ReservationPolicyTest {

    private final ReservationPolicy reservationPolicy = new ReservationPolicy(new TicketSalesPolicy());

    @Test
    void allowsReservationForSellableTicketWithAvailableInventory() {
        TicketType ticketType = ticketType(true, 100);

        reservationPolicy.assertCanReserve(
                ticketType,
                ticketType.getInventory(),
                2,
                Instant.parse("2026-08-15T00:00:00Z"));
    }

    @Test
    void rejectsReservationWhenTicketIsInactiveOrSoldOut() {
        TicketType inactiveTicket = ticketType(false, 100);
        TicketType soldOutTicket = ticketType(true, 2);
        soldOutTicket.getInventory().reserve(2);

        assertThatThrownBy(() -> reservationPolicy.assertCanReserve(
                inactiveTicket,
                inactiveTicket.getInventory(),
                1,
                Instant.parse("2026-08-15T00:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Ticket type is not available for reservation.");

        assertThatThrownBy(() -> reservationPolicy.assertCanReserve(
                soldOutTicket,
                soldOutTicket.getInventory(),
                1,
                Instant.parse("2026-08-15T00:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Not enough inventory available.");
    }

    @Test
    void enforcesExpiryOrdering() {
        assertThatThrownBy(() -> reservationPolicy.assertExpiryOrdering(
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-08-15T00:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Expiry must be after the current time.");
    }

    @Test
    void rejectsExpiredOrderTransitions() {
        TicketType ticketType = ticketType(true, 100);
        Order order = new Order(
                TestEntityFactory.user("buyer@example.com"),
                ticketType.getEvent(),
                Instant.parse("2026-08-15T00:00:00Z"));

        assertThatThrownBy(() -> reservationPolicy.assertCanConfirm(order, Instant.parse("2026-08-15T00:00:01Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Order has expired.");

        assertThatThrownBy(() -> reservationPolicy.assertCanExpire(order, Instant.parse("2026-08-14T23:59:59Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Order has not expired yet.");
    }

    private TicketType ticketType(boolean active, int totalQuantity) {
        return TestEntityFactory.ticketType(
                TestEntityFactory.event(
                        TestEntityFactory.user("organizer@example.com"),
                        TestEntityFactory.category("Concerts", "concerts"),
                        TestEntityFactory.venue("Palace", "Kyiv"),
                        "rock-night",
                        nure.com.multievent.catalog.domain.EventStatus.PUBLISHED),
                active,
                totalQuantity,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
