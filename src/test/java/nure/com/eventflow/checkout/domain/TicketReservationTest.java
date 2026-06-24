package nure.com.eventflow.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketType;
import org.junit.jupiter.api.Test;

class TicketReservationTest {

    @Test
    void tracksStatusTransitionsAndQuantity() {
        TicketType ticketType = activeTicketType();
        TicketReservation reservation = new TicketReservation(
                TicketingTestFixtures.user("buyer@example.com", java.util.UUID.randomUUID()),
                ticketType.getEvent(),
                Instant.parse("2026-08-15T00:15:00Z"));
        reservation.addItem(new TicketReservationItem(ticketType, 2));

        assertThat(reservation.getTotalQuantity()).isEqualTo(2);
        reservation.changeQuantity(3);
        assertThat(reservation.getTotalQuantity()).isEqualTo(3);

        reservation.confirm(Instant.parse("2026-08-15T00:05:00Z"));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void rejectsNonPendingTransitions() {
        TicketType ticketType = activeTicketType();
        TicketReservation reservation = new TicketReservation(
                TicketingTestFixtures.user("buyer@example.com", java.util.UUID.randomUUID()),
                ticketType.getEvent(),
                Instant.parse("2026-08-15T00:15:00Z"));
        reservation.addItem(new TicketReservationItem(ticketType, 1));
        reservation.release(Instant.parse("2026-08-15T00:05:00Z"));

        assertThatThrownBy(() -> reservation.confirm(Instant.parse("2026-08-15T00:06:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Reservation is no longer pending.");
    }

    private TicketType activeTicketType() {
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
                100,
                0,
                0);
    }
}
