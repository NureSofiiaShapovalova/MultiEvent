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
import nure.com.eventflow.checkout.domain.TicketReservationPolicy;
import nure.com.eventflow.checkout.infra.TicketReservationRepository;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.ticketing.TicketingTestFixtures;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdjustTicketReservationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private Clock clock;

    private AdjustTicketReservationService adjustTicketReservationService;

    @BeforeEach
    void setUp() {
        adjustTicketReservationService = new AdjustTicketReservationService(
                userRepository,
                ticketReservationRepository,
                ticketInventoryRepository,
                new TicketReservationPolicy(new nure.com.eventflow.ticketing.domain.TicketSalesPolicy()),
                clock);
    }

    @Test
    void adjustsQuantityAndInventoryReservation() {
        User customer = CheckoutTestFixtures.customer("buyer@example.com", UUID.randomUUID());
        Event event = publishedEvent();
        TicketType ticketType = CheckoutTestFixtures.ticketType(event, true);
        TicketReservation reservation = new TicketReservation(customer, event, Instant.parse("2026-08-15T00:15:00Z"));
        reservation.addItem(new nure.com.eventflow.checkout.domain.TicketReservationItem(ticketType, 2));
        ticketType.getInventory().reserve(2);
        ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(ticketReservationRepository.findByIdAndCustomer_Id(reservation.getId(), customer.getId())).thenReturn(Optional.of(reservation));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-15T00:00:00Z"));

        adjustTicketReservationService.adjust("buyer@example.com", reservation.getId(), new AdjustReservationCommand(4));

        assertThat(reservation.getTotalQuantity()).isEqualTo(4);
        assertThat(ticketType.getInventory().getReservedQuantity()).isEqualTo(4);
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
