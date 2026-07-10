package nure.com.multievent.reporting.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.domain.Venue;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.catalog.infra.VenueRepository;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderItem;
import nure.com.multievent.checkout.infra.OrderRepository;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.iam.infra.RoleRepository;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import(ReportingQueryRepository.class)
class ReportingQueryRepositoryIntegrationTest {

    @Autowired
    private ReportingQueryRepository reportingQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void aggregatesAdminAndOrganizerReports() {
        User organizer = user("organizer@example.com", RoleName.ROLE_USER, RoleName.ROLE_ORGANIZER);
        User buyer = user("buyer@example.com", RoleName.ROLE_USER);
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));
        Event event = event(organizer, category, venue);
        TicketType ticketType = ticketType(event);

        Order paidOrder = new Order(buyer, event, Instant.parse("2026-08-15T00:10:00Z"));
        paidOrder.addItem(new OrderItem(ticketType, 2));
        paidOrder.updateTotals(new BigDecimal("50.00"), "USD");
        paidOrder.markPaid();
        orderRepository.saveAndFlush(paidOrder);

        Order pendingOrder = new Order(buyer, event, Instant.parse("2026-08-15T00:20:00Z"));
        pendingOrder.updateTotals(new BigDecimal("25.00"), "USD");
        orderRepository.saveAndFlush(pendingOrder);

        Payment failedPayment = new Payment(
                buyer,
                pendingOrder,
                "LOCAL_MANUAL",
                "local-ref",
                new BigDecimal("25.00"),
                "USD",
                Instant.parse("2026-08-15T00:12:00Z"));
        failedPayment.markFailed("Declined", Instant.parse("2026-08-15T00:13:00Z"));
        paymentRepository.saveAndFlush(failedPayment);

        entityManager.clear();

        var adminReport = reportingQueryRepository.adminKpis();
        assertThat(adminReport.organizers()).isEqualTo(1L);
        assertThat(adminReport.paidOrders()).isEqualTo(1L);
        assertThat(adminReport.pendingOrders()).isEqualTo(1L);
        assertThat(adminReport.failedPayments()).isEqualTo(1L);
        assertThat(adminReport.grossRevenue()).isEqualByComparingTo("50.00");
        assertThat(adminReport.categoryPopularity())
                .extracting(row -> row.categoryName())
                .contains("Concerts");

        var organizerReport = reportingQueryRepository.organizerReports(organizer.getId());
        assertThat(organizerReport.paidOrders()).isEqualTo(1L);
        assertThat(organizerReport.ticketsSold()).isEqualTo(2L);
        assertThat(organizerReport.revenue()).isEqualByComparingTo("50.00");
        assertThat(organizerReport.revenueRows())
                .extracting(row -> row.eventTitle())
                .containsExactly("Rock Night");
    }

    private User user(String email, RoleName... roleNames) {
        Role primary = role(roleNames[0]);
        User user = User.register(email, "{noop}secret", primary, new UserProfile(null, null, null, null, null));
        for (int index = 1; index < roleNames.length; index++) {
            user.assignRole(role(roleNames[index]));
        }
        return userRepository.saveAndFlush(user);
    }

    private Role role(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(roleName)));
    }

    private Event event(User organizer, Category category, Venue venue) {
        Event event = new Event(
                organizer,
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
        return event;
    }

    private TicketType ticketType(Event event) {
        TicketType ticketType = new TicketType(
                event,
                "Standard",
                "Entry ticket",
                new BigDecimal("25.00"),
                "USD",
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                true);
        ticketType.attachInventory(new TicketInventory(ticketType, 100));
        return ticketTypeRepository.saveAndFlush(ticketType);
    }
}
