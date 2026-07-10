package nure.com.multievent.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.checkout.CheckoutTestFixtures;
import nure.com.multievent.checkout.domain.TicketReservation;
import nure.com.multievent.checkout.infra.TicketReservationRepository;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.ticketing.TicketingTestFixtures;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
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
                new nure.com.multievent.checkout.domain.TicketReservationPolicy(new nure.com.multievent.ticketing.domain.TicketSalesPolicy()),
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
