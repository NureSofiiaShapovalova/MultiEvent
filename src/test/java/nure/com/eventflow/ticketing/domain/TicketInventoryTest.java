package nure.com.eventflow.ticketing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import org.junit.jupiter.api.Test;

class TicketInventoryTest {

    @Test
    void computesAvailableQuantityAndUpdatesReservations() {
        TicketType ticketType = TicketingTestFixtures.ticketType(
                TicketingTestFixtures.event(
                        TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                        TicketingTestFixtures.category("Concerts", "concerts"),
                        TicketingTestFixtures.venue("Palace", "Kyiv"),
                        EventStatus.DRAFT),
                "Standard",
                true,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                BigDecimal.valueOf(25),
                "USD",
                4,
                100,
                10,
                5);

        TicketInventory inventory = ticketType.getInventory();
        assertThat(inventory.availableQuantity()).isEqualTo(85);

        inventory.reserve(5);
        assertThat(inventory.getReservedQuantity()).isEqualTo(15);
        assertThat(inventory.availableQuantity()).isEqualTo(80);

        inventory.release(3);
        assertThat(inventory.getReservedQuantity()).isEqualTo(12);

        inventory.markSold(4);
        assertThat(inventory.getReservedQuantity()).isEqualTo(8);
        assertThat(inventory.getSoldQuantity()).isEqualTo(9);
    }

    @Test
    void rejectsQuantityChangesThatWouldGoNegative() {
        TicketType ticketType = TicketingTestFixtures.ticketType(
                TicketingTestFixtures.event(
                        TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                        TicketingTestFixtures.category("Concerts", "concerts"),
                        TicketingTestFixtures.venue("Palace", "Kyiv"),
                        EventStatus.DRAFT),
                "Standard",
                true,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                BigDecimal.valueOf(25),
                "USD",
                4,
                10,
                8,
                0);

        assertThatThrownBy(() -> ticketType.getInventory().changeTotalQuantity(7))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Total quantity cannot be lower than reserved and sold inventory.");
    }
}
