package nure.com.multievent.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.checkout.CheckoutTestFixtures;
import nure.com.multievent.checkout.domain.TicketReservation;
import nure.com.multievent.checkout.domain.TicketReservationPolicy;
import nure.com.multievent.checkout.infra.TicketReservationRepository;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.ticketing.TicketingTestFixtures;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReleaseTicketReservationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private Clock clock;

    private ReleaseTicketReservationService releaseTicketReservationService;

    @BeforeEach
    void setUp() {
        releaseTicketReservationService = new ReleaseTicketReservationService(
                userRepository,
                ticketReservationRepository,
                ticketInventoryRepository,
                new TicketReservationPolicy(new nure.com.multievent.ticketing.domain.TicketSalesPolicy()),
                clock);
    }

    @Test
    void releasesReservationAndRestoresInventory() {
        User customer = CheckoutTestFixtures.customer("buyer@example.com", UUID.randomUUID());
        Event event = publishedEvent();
        TicketType ticketType = CheckoutTestFixtures.ticketType(event, true);
        TicketReservation reservation = new TicketReservation(customer, event, Instant.parse("2026-08-15T00:15:00Z"));
        reservation.addItem(new nure.com.multievent.checkout.domain.TicketReservationItem(ticketType, 2));
        ticketType.getInventory().reserve(2);
        ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(ticketReservationRepository.findByIdAndCustomer_Id(reservation.getId(), customer.getId())).thenReturn(Optional.of(reservation));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-15T00:05:00Z"));

        releaseTicketReservationService.release("buyer@example.com", reservation.getId());

        assertThat(reservation.getStatus()).isEqualTo(nure.com.multievent.checkout.domain.ReservationStatus.RELEASED);
        assertThat(ticketType.getInventory().getReservedQuantity()).isEqualTo(0);
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
