package nure.com.multievent.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.infra.EventCatalogQueryRepository;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderPricingService;
import nure.com.multievent.checkout.domain.OrderStatus;
import nure.com.multievent.checkout.domain.ReservationPolicy;
import nure.com.multievent.checkout.infra.OrderRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.ticketing.domain.TicketSalesPolicy;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateOrderFromSelectionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventCatalogQueryRepository eventCatalogQueryRepository;

    @Mock
    private TicketingQueryRepository ticketingQueryRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Clock clock;

    private CreateOrderFromSelectionService createOrderFromSelectionService;

    @BeforeEach
    void setUp() {
        createOrderFromSelectionService = new CreateOrderFromSelectionService(
                userRepository,
                eventCatalogQueryRepository,
                ticketingQueryRepository,
                ticketInventoryRepository,
                orderRepository,
                new ReservationPolicy(new TicketSalesPolicy()),
                new OrderPricingService(),
                clock);
    }

    @Test
    void createsPendingPaymentOrderAndReservesInventory() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User buyer = TestEntityFactory.user("buyer@example.com");
        Event event = publishedEvent();
        TicketType ticketType = TestEntityFactory.ticketType(
                event,
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
        when(eventCatalogQueryRepository.findPublishedEventBySlug("rock-night")).thenReturn(Optional.of(event));
        when(ticketingQueryRepository.findPublicSellableTicketTypesByEventSlug("rock-night", now))
                .thenReturn(List.of(ticketType));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            TestEntityFactory.setId(order, UUID.randomUUID());
            return order;
        });
        when(ticketInventoryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        UUID orderId = createOrderFromSelectionService.create(
                "buyer@example.com",
                "rock-night",
                new TicketSelectionCommand(List.of(new TicketSelectionCommand.Selection(ticketType.getId(), 2))));

        assertThat(orderId).isNotNull();
        assertThat(ticketType.getInventory().getReservedQuantity()).isEqualTo(2);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("50.00");
        assertThat(savedOrder.getCurrency()).isEqualTo("USD");
        assertThat(savedOrder.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getReservationLocks()).hasSize(1);
    }

    private Event publishedEvent() {
        return TestEntityFactory.event(
                TestEntityFactory.user("organizer@example.com"),
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.PUBLISHED);
    }
}
