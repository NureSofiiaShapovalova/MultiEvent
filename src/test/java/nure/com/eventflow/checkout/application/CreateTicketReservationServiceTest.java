package nure.com.eventflow.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.checkout.CheckoutTestFixtures;
import nure.com.eventflow.checkout.domain.TicketReservation;
import nure.com.eventflow.checkout.infra.TicketReservationRepository;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketInventoryRepository;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateTicketReservationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @Mock
    private Clock clock;

    private CreateTicketReservationService createTicketReservationService;

    @BeforeEach
    void setUp() {
        createTicketReservationService = new CreateTicketReservationService(
                userRepository,
                ticketingQueryRepository,
                ticketInventoryRepository,
                ticketReservationRepository,
                new nure.com.eventflow.checkout.domain.TicketReservationPolicy(new nure.com.eventflow.ticketing.domain.TicketSalesPolicy()),
                clock);
    }

    @Test
    void createsReservationAndReservesInventory() {
        User customer = CheckoutTestFixtures.customer("buyer@example.com", UUID.randomUUID());
        Event event = publishedEvent();
        TicketType ticketType = CheckoutTestFixtures.ticketType(event, true);
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(ticketingQueryRepository.findPublicSellableTicketTypeBySlugAndId("rock-night", ticketType.getId(), now))
                .thenReturn(Optional.of(ticketType));
        when(ticketReservationRepository.save(any(TicketReservation.class))).thenAnswer(invocation -> {
            TicketReservation reservation = invocation.getArgument(0);
            ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(reservation.getPrimaryItem(), "id", UUID.randomUUID());
            return reservation;
        });
        when(clock.instant()).thenReturn(now);

        UUID reservationId = createTicketReservationService.create(
                "buyer@example.com",
                "rock-night",
                new CreateReservationCommand(ticketType.getId(), 2));

        assertThat(reservationId).isNotNull();
        assertThat(ticketType.getInventory().getReservedQuantity()).isEqualTo(2);
        assertThat(ticketType.getInventory().availableQuantity()).isEqualTo(98);
    }

    private Event publishedEvent() {
        User organizer = TicketingTestFixtures.user("organizer@example.com", UUID.randomUUID());
        return TicketingTestFixtures.event(
                organizer,
                TicketingTestFixtures.category("Concerts", "concerts"),
                TicketingTestFixtures.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED);
    }
}
