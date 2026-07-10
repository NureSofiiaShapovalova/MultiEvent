package nure.com.multievent.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;

class OrderPricingServiceTest {

    private final OrderPricingService orderPricingService = new OrderPricingService();

    @Test
    void calculatesOrderTotalFromLineItems() {
        TicketType ticketType = ticketType();
        OrderItem standard = new OrderItem(ticketType, 2);
        OrderItem vip = new OrderItem(ticketType, 1);

        assertThat(orderPricingService.calculateTotal(List.of(standard, vip)))
                .isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void returnsZeroForEmptyOrder() {
        assertThat(orderPricingService.calculateTotal(List.of()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private TicketType ticketType() {
        return TestEntityFactory.ticketType(
                TestEntityFactory.event(
                        TestEntityFactory.user("organizer@example.com"),
                        TestEntityFactory.category("Concerts", "concerts"),
                        TestEntityFactory.venue("Palace", "Kyiv"),
                        "rock-night",
                        nure.com.multievent.catalog.domain.EventStatus.PUBLISHED),
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
