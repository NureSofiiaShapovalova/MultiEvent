package nure.com.eventflow.checkout.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.VenueRepository;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.domain.OrderItem;
import nure.com.eventflow.checkout.domain.OrderStatus;
import nure.com.eventflow.checkout.domain.ReservationLock;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import(CheckoutQueryRepository.class)
class CheckoutQueryRepositoryIntegrationTest {

    @Autowired
    private CheckoutQueryRepository checkoutQueryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void fetchesOrderGraphAndPendingOrdersForCustomer() {
        User customer = customer("buyer@example.com");
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));
        Event event = new Event(
                customer,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                EventStatus.PUBLISHED,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z"));
        event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        entityManager.persist(event);
        entityManager.flush();

        TicketType ticketType = new TicketType(
                event,
                "Standard",
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                true);
        ticketType.attachInventory(new TicketInventory(ticketType, 100));
        ticketTypeRepository.saveAndFlush(ticketType);

        Order order = new Order(customer, event, Instant.parse("2026-08-15T00:10:00Z"));
        order.addItem(new OrderItem(ticketType, 2));
        order.addReservationLock(new ReservationLock(ticketType, 2, order.getExpiresAt()));
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        orderRepository.saveAndFlush(order);

        entityManager.clear();

        Order loaded = checkoutQueryRepository.findUserOrderById(customer.getId(), order.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getReservationLocks()).hasSize(1);
        assertThat(loaded.getReservationLocks().get(0).getTicketType().getInventory().getTotalQuantity()).isEqualTo(100);
        assertThat(checkoutQueryRepository.findUserOrders(customer.getId())).hasSize(1);
        assertThat(checkoutQueryRepository.findPendingOrdersToExpire(Instant.parse("2026-08-15T00:15:00Z")))
                .extracting(Order::getId)
                .contains(order.getId());
    }

    @Test
    void findsOrdersByEventForCancellationNotifications() {
        User customerA = customer("buyer-a@example.com");
        User customerB = customer("buyer-b@example.com");
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));
        Event event = new Event(
                customerA,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                EventStatus.PUBLISHED,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z"));
        event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        entityManager.persist(event);
        entityManager.flush();

        Order pendingOrder = new Order(customerA, event, Instant.parse("2026-08-15T00:10:00Z"));
        orderRepository.saveAndFlush(pendingOrder);

        Order paidOrder = new Order(customerB, event, Instant.parse("2026-08-15T00:20:00Z"));
        paidOrder.markPaid();
        orderRepository.saveAndFlush(paidOrder);

        Order cancelledOrder = new Order(customerB, event, Instant.parse("2026-08-15T00:30:00Z"));
        cancelledOrder.cancel();
        orderRepository.saveAndFlush(cancelledOrder);

        entityManager.clear();

        List<Order> orders = checkoutQueryRepository.findOrdersByEventId(event.getId());
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(order -> order.getCustomer().getEmail())
                .containsExactlyInAnyOrder("buyer-a@example.com", "buyer-b@example.com");
        assertThat(orders).extracting(Order::getStatus)
                .containsExactlyInAnyOrder(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
    }

    private User customer(String email) {
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER)));
        return userRepository.saveAndFlush(User.register(email, "{noop}secret", role, new UserProfile(null, null, null, null, null)));
    }
}
