package nure.com.eventflow.checkout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUserOrdersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CheckoutQueryRepository checkoutQueryRepository;

    private ListUserOrdersService listUserOrdersService;

    @BeforeEach
    void setUp() {
        listUserOrdersService = new ListUserOrdersService(
                userRepository,
                checkoutQueryRepository,
                new CheckoutViewMapper(new TicketSalesPolicy()));
    }

    @Test
    void listsOnlyOrdersForCurrentUser() {
        User buyer = TestEntityFactory.user("buyer@example.com");
        Order order = pendingOrder();
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-15T00:01:00Z");
        Instant updatedAt = Instant.parse("2026-08-15T00:02:00Z");
        TestEntityFactory.setId(order, orderId);
        TestEntityFactory.setInstantFields(order, createdAt, updatedAt);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
        when(checkoutQueryRepository.findUserOrders(buyer.getId())).thenReturn(List.of(order));

        List<UserOrderRowView> orders = listUserOrdersService.list("buyer@example.com");

        assertThat(orders).hasSize(1);
        UserOrderRowView row = orders.get(0);
        assertThat(row.orderId()).isEqualTo(orderId);
        assertThat(row.totalAmount()).isEqualByComparingTo("50.00");
        assertThat(row.itemCount()).isEqualTo(1);
        assertThat(row.totalQuantity()).isEqualTo(2);
        assertThat(row.createdAt()).isEqualTo(createdAt);
    }

    private Order pendingOrder() {
        TicketType ticketType = ticketType();
        Order order = new Order(
                TestEntityFactory.user("buyer@example.com"),
                ticketType.getEvent(),
                Instant.parse("2026-08-15T00:15:00Z"));
        order.addItem(new nure.com.eventflow.checkout.domain.OrderItem(ticketType, 2));
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
