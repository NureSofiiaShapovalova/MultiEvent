package nure.com.eventflow.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPublicTicketOptionsServiceTest {

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private Clock clock;

    private GetPublicTicketOptionsService getPublicTicketOptionsService;

    @BeforeEach
    void setUp() {
        getPublicTicketOptionsService = new GetPublicTicketOptionsService(
                ticketingQueryRepository,
                new TicketingViewMapper(new TicketSalesPolicy()),
                clock);
    }

    @Test
    void mapsActiveTicketsToPublicOptions() {
        var organizer = TicketingTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID());
        var event = TicketingTestFixtures.event(
                organizer,
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED);
        TicketType available = TicketingTestFixtures.ticketType(
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

        when(ticketingQueryRepository.findPublicSellableTicketTypesByEventSlug("rock-night", now)).thenReturn(List.of(available));
        when(clock.instant()).thenReturn(now);

        List<TicketOptionView> options = getPublicTicketOptionsService.getByEventSlug("rock-night");

        assertThat(options).hasSize(1);
        assertThat(options.get(0).purchasable()).isTrue();
        assertThat(options.get(0).availabilityLabel()).isEqualTo("On sale");
    }
}
