package nure.com.eventflow.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
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
class ListOrganizerTicketTypesServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private Clock clock;

    private ListOrganizerTicketTypesService listOrganizerTicketTypesService;

    @BeforeEach
    void setUp() {
        listOrganizerTicketTypesService = new ListOrganizerTicketTypesService(
                userRepository,
                ticketingQueryRepository,
                new TicketingViewMapper(new TicketSalesPolicy()),
                clock);
    }

    @Test
    void listsOwnedTicketTypesAndLoadsEditRow() {
        User organizer = TicketingTestFixtures.user("organizer@example.com", UUID.randomUUID());
        Event event = TicketingTestFixtures.event(
                organizer,
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED);
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
                10,
                5);
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(ticketingQueryRepository.findOrganizerTicketTypes(organizer.getId(), event.getId())).thenReturn(List.of(ticketType));
        when(ticketingQueryRepository.findOrganizerTicketType(organizer.getId(), event.getId(), ticketType.getId())).thenReturn(Optional.of(ticketType));
        when(clock.instant()).thenReturn(now);

        List<OrganizerTicketRowView> rows = listOrganizerTicketTypesService.list("organizer@example.com", event.getId());
        OrganizerTicketRowView editRow = listOrganizerTicketTypesService.getForEdit("organizer@example.com", event.getId(), ticketType.getId());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).availabilityLabel()).isEqualTo("On sale");
        assertThat(editRow.availableQuantity()).isEqualTo(85);
        assertThat(editRow.eventTitle()).isEqualTo("Rock Night");
    }
}
