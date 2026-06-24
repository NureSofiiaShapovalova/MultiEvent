package nure.com.eventflow.checkout.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import org.junit.jupiter.api.Test;

class TicketReservationPolicyTest {

    private final TicketReservationPolicy policy = new TicketReservationPolicy(new TicketSalesPolicy());

    @Test
    void validatesReservationCreationAndExpiryOrdering() {
        TicketType ticketType = activeTicketType(100);
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        policy.assertCanCreateReservation(ticketType, ticketType.getInventory(), 2, now);
        policy.assertExpiryOrdering(now.plusSeconds(900), now);
    }

    @Test
    void rejectsBadExpiryOrdering() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        assertThatThrownBy(() -> policy.assertExpiryOrdering(now, now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Expiry must be after the current time.");
    }

    @Test
    void rejectsExpiredOrOversizedReservationChanges() {
        TicketType ticketType = activeTicketType(2);
        TicketReservation reservation = new TicketReservation(
                TicketingTestFixtures.user("buyer@example.com", java.util.UUID.randomUUID()),
                ticketType.getEvent(),
                Instant.parse("2026-08-15T00:15:00Z"));
        reservation.addItem(new TicketReservationItem(ticketType, 1));

        assertThatThrownBy(() -> policy.assertCanCreateReservation(ticketType, ticketType.getInventory(), 5, Instant.parse("2026-08-15T00:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Quantity cannot exceed max per order.");

        assertThatThrownBy(() -> policy.assertCanAdjustReservation(reservation, ticketType.getInventory(), 3, Instant.parse("2026-08-15T00:20:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Reservation has expired.");
    }

    private TicketType activeTicketType(int availableQuantity) {
        var organizer = TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID());
        var event = TicketingTestFixtures.event(
                organizer,
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED);
        return TicketingTestFixtures.ticketType(
                event,
                "Standard",
                true,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                BigDecimal.valueOf(25),
                "USD",
                4,
                availableQuantity,
                0,
                0);
    }
}
