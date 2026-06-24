package nure.com.eventflow.ticketing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import org.junit.jupiter.api.Test;

class TicketSalesPolicyTest {

    private final TicketSalesPolicy policy = new TicketSalesPolicy();

    @Test
    void validatesSalesWindowAndAvailabilityLabels() {
        policy.assertValidSalesWindow(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
        policy.assertValidEventState(TicketingTestFixtures.event(
                TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED));

        TicketType ticketType = TicketingTestFixtures.ticketType(
                TicketingTestFixtures.event(
                        TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                        TicketingTestFixtures.category("Concerts", "concerts"),
                        TicketingTestFixtures.venue("Palace", "Kyiv"),
                        EventStatus.PUBLISHED),
                "Standard",
                true,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                BigDecimal.valueOf(25),
                "USD",
                4,
                10,
                0,
                0);

        assertThat(policy.isSellable(ticketType, Instant.parse("2026-08-15T00:00:00Z"))).isTrue();
        assertThat(policy.availabilityLabel(ticketType, ticketType.getInventory(), Instant.parse("2026-08-15T00:00:00Z"))).isEqualTo("On sale");
    }

    @Test
    void rejectsInvalidSalesWindowAndInactiveTickets() {
        assertThatThrownBy(() -> policy.assertValidSalesWindow(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Sales end must be after sales start.");

        TicketType ticketType = TicketingTestFixtures.ticketType(
                TicketingTestFixtures.event(
                        TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                        TicketingTestFixtures.category("Concerts", "concerts"),
                        TicketingTestFixtures.venue("Palace", "Kyiv"),
                        EventStatus.PUBLISHED),
                "Standard",
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                BigDecimal.valueOf(25),
                "USD",
                4,
                10,
                0,
                0);

        assertThat(policy.isSellable(ticketType, Instant.parse("2026-08-15T00:00:00Z"))).isFalse();
        assertThat(policy.availabilityLabel(ticketType, ticketType.getInventory(), Instant.parse("2026-08-15T00:00:00Z"))).isEqualTo("Inactive");
    }
}
