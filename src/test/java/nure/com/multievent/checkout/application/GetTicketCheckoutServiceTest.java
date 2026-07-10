package nure.com.multievent.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.ticketing.TicketingTestFixtures;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTicketCheckoutServiceTest {

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private Clock clock;

    private GetTicketCheckoutService getTicketCheckoutService;

    @BeforeEach
    void setUp() {
        getTicketCheckoutService = new GetTicketCheckoutService(ticketingQueryRepository, new CheckoutViewMapper(new nure.com.multievent.ticketing.domain.TicketSalesPolicy()), clock);
    }

    @Test
    void loadsSellableTicketCheckoutView() {
        Event event = publishedEvent();
        TicketType ticketType = TicketingTestFixtures.ticketType(
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
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        when(ticketingQueryRepository.findPublicSellableTicketTypeBySlugAndId("rock-night", ticketType.getId(), now))
                .thenReturn(Optional.of(ticketType));
        when(clock.instant()).thenReturn(now);

        TicketCheckoutView view = getTicketCheckoutService.getBySlugAndTicketTypeId("rock-night", ticketType.getId());

        assertThat(view.ticketName()).isEqualTo("Standard");
        assertThat(view.availableQuantity()).isEqualTo(100);
    }

    private Event publishedEvent() {
        var organizer = TicketingTestFixtures.user("organizer@example.com", UUID.randomUUID());
        return TicketingTestFixtures.event(
                organizer,
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED);
    }
}
