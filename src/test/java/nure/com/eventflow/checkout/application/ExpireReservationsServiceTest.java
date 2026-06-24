package nure.com.eventflow.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.domain.OrderItem;
import nure.com.eventflow.checkout.domain.OrderStatus;
import nure.com.eventflow.checkout.domain.ReservationLock;
import nure.com.eventflow.checkout.domain.ReservationPolicy;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketInventoryRepository;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpireReservationsServiceTest {

    @Mock
    private CheckoutQueryRepository checkoutQueryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private Clock clock;

    private ExpireReservationsService expireReservationsService;

    @BeforeEach
    void setUp() {
        expireReservationsService = new ExpireReservationsService(
                checkoutQueryRepository,
                orderRepository,
                ticketInventoryRepository,
                new ReservationPolicy(new TicketSalesPolicy()),
                clock);
    }

    @Test
    void expiresPendingOrdersAndReleasesInventory() {
        Instant now = Instant.parse("2026-08-15T00:15:00Z");
        Order order = expiredOrder(now);
        TicketInventory inventory = order.getReservationLocks().get(0).getTicketType().getInventory();
        inventory.reserve(2);

        when(clock.instant()).thenReturn(now);
        when(checkoutQueryRepository.findPendingOrdersToExpire(now)).thenReturn(List.of(order));
        when(orderRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketInventoryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        expireReservationsService.expireDueReservations();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.getReservationLocks().get(0).getStatus()).isEqualTo(nure.com.eventflow.checkout.domain.ReservationStatus.EXPIRED);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(orderRepository).saveAll(List.of(order));
        verify(ticketInventoryRepository).saveAll(List.of(inventory));
    }

    private Order expiredOrder(Instant now) {
        TicketType ticketType = ticketType();
        User buyer = TestEntityFactory.user("buyer@example.com");
        Order order = new Order(buyer, ticketType.getEvent(), now.minusSeconds(60));
        order.addItem(new OrderItem(ticketType, 2));
        order.addReservationLock(new ReservationLock(ticketType, 2, order.getExpiresAt()));
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        return order;
    }

    private TicketType ticketType() {
        return TestEntityFactory.ticketType(
                TestEntityFactory.event(
                        TestEntityFactory.user("organizer@example.com"),
                        TestEntityFactory.category("Concerts", "concerts"),
                        TestEntityFactory.venue("Palace", "Kyiv"),
                        "rock-night",
                        nure.com.eventflow.catalog.domain.EventStatus.PUBLISHED),
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
    }
}
