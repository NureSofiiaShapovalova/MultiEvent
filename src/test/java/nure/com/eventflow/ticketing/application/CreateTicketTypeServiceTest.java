package nure.com.eventflow.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketPricingPolicy;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateTicketTypeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizerEventRepository organizerEventRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Clock clock;

    private CreateTicketTypeService createTicketTypeService;

    @BeforeEach
    void setUp() {
        createTicketTypeService = new CreateTicketTypeService(
                userRepository,
                organizerEventRepository,
                ticketTypeRepository,
                auditLogRepository,
                new TicketSalesPolicy(),
                new TicketPricingPolicy(),
                clock);
    }

    @Test
    void createsTicketTypeWithInventoryAndAuditLog() {
        User organizer = TicketingTestFixtures.user("organizer@example.com", UUID.randomUUID());
        Event event = TicketingTestFixtures.event(
                organizer,
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.DRAFT);
        Instant now = Instant.parse("2026-06-22T12:00:00Z");

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(organizerEventRepository.findByIdAndOrganizer_Id(event.getId(), organizer.getId())).thenReturn(Optional.of(event));
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> {
            TicketType ticketType = invocation.getArgument(0);
            ReflectionTestUtils.setField(ticketType, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(ticketType.getInventory(), "id", UUID.randomUUID());
            return ticketType;
        });
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        UUID ticketTypeId = createTicketTypeService.create(
                "organizer@example.com",
                event.getId(),
                new CreateTicketTypeCommand(
                        "Standard",
                        "Entry ticket",
                        BigDecimal.valueOf(25),
                        "usd",
                        4,
                        100,
                        Instant.parse("2026-08-01T00:00:00Z"),
                        Instant.parse("2026-09-01T00:00:00Z")));

        assertThat(ticketTypeId).isNotNull();

        ArgumentCaptor<TicketType> ticketCaptor = ArgumentCaptor.forClass(TicketType.class);
        org.mockito.Mockito.verify(ticketTypeRepository).save(ticketCaptor.capture());
        TicketType savedTicketType = ticketCaptor.getValue();
        assertThat(savedTicketType.isActive()).isTrue();
        assertThat(savedTicketType.getInventory().getTotalQuantity()).isEqualTo(100);
        assertThat(savedTicketType.getInventory().availableQuantity()).isEqualTo(100);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("CREATE_TICKET_TYPE");
        assertThat(auditCaptor.getValue().getTargetId()).isEqualTo(ticketTypeId);
    }
}
