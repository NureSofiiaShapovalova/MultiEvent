package nure.com.eventflow.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketPricingPolicy;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import nure.com.eventflow.ticketing.infra.TicketTypeRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeactivateTicketTypeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private DeactivateTicketTypeService deactivateTicketTypeService;

    @BeforeEach
    void setUp() {
        deactivateTicketTypeService = new DeactivateTicketTypeService(
                userRepository,
                ticketingQueryRepository,
                ticketTypeRepository,
                auditLogRepository,
                new TicketSalesPolicy());
    }

    @Test
    void deactivatesTicketTypeAndRecordsAuditLog() {
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
                java.math.BigDecimal.valueOf(25),
                "USD",
                4,
                100,
                0,
                0);

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(ticketingQueryRepository.findOrganizerTicketType(organizer.getId(), event.getId(), ticketType.getId()))
                .thenReturn(Optional.of(ticketType));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deactivateTicketTypeService.deactivate("organizer@example.com", event.getId(), ticketType.getId());

        assertThat(ticketType.isActive()).isFalse();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DEACTIVATE_TICKET_TYPE");
    }
}
