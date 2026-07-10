package nure.com.multievent.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.ticketing.TicketingTestFixtures;
import nure.com.multievent.ticketing.domain.TicketPricingPolicy;
import nure.com.multievent.ticketing.domain.TicketSalesPolicy;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
import nure.com.multievent.ticketing.infra.TicketTypeRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateTicketTypeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private UpdateTicketTypeService updateTicketTypeService;

    @BeforeEach
    void setUp() {
        updateTicketTypeService = new UpdateTicketTypeService(
                userRepository,
                ticketingQueryRepository,
                ticketTypeRepository,
                auditLogRepository,
                new TicketSalesPolicy(),
                new TicketPricingPolicy());
    }

    @Test
    void updatesTicketTypeAndInventoryTotal() {
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
                java.time.Instant.parse("2026-08-01T00:00:00Z"),
                java.time.Instant.parse("2026-09-01T00:00:00Z"),
                BigDecimal.valueOf(25),
                "USD",
                4,
                100,
                10,
                5);

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(ticketingQueryRepository.findOrganizerTicketType(organizer.getId(), event.getId(), ticketType.getId()))
                .thenReturn(Optional.of(ticketType));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        updateTicketTypeService.update(
                "organizer@example.com",
                event.getId(),
                ticketType.getId(),
                new UpdateTicketTypeCommand(
                        "VIP",
                        "Better seats",
                        BigDecimal.valueOf(50),
                        "eur",
                        2,
                        120,
                        java.time.Instant.parse("2026-08-05T00:00:00Z"),
                        java.time.Instant.parse("2026-09-05T00:00:00Z")));

        assertThat(ticketType.getName()).isEqualTo("VIP");
        assertThat(ticketType.getPrice()).isEqualByComparingTo("50");
        assertThat(ticketType.getCurrency()).isEqualTo("EUR");
        assertThat(ticketType.getInventory().getTotalQuantity()).isEqualTo(120);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("UPDATE_TICKET_TYPE");
    }
}
