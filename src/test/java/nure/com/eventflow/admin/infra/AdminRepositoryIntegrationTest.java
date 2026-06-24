package nure.com.eventflow.admin.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.admin.domain.ModerationAction;
import nure.com.eventflow.admin.domain.ModerationActionType;
import nure.com.eventflow.admin.application.AdminViewMapper;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.catalog.infra.VenueRepository;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentAttempt;
import nure.com.eventflow.payment.domain.PaymentAttemptType;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AdminRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ModerationActionRepository moderationActionRepository;

    @Test
    void readsUsersAndRoleCounts() {
        Role userRole = role(RoleName.ROLE_USER);
        Role organizerRole = role(RoleName.ROLE_ORGANIZER);
        User regular = user("regular@example.com", userRole);
        User organizer = user("organizer@example.com", userRole, organizerRole);
        user("admin@example.com", userRole, role(RoleName.ROLE_ADMIN));

        assertThat(userRepository.countByRoleName(RoleName.ROLE_ORGANIZER)).isEqualTo(1L);
        assertThat(userRepository.findDistinctByRoleNameOrderByEmailAsc(RoleName.ROLE_ORGANIZER))
                .extracting(User::getEmail)
                .containsExactly("organizer@example.com");
        assertThat(userRepository.findAllByOrderByEmailAsc())
                .extracting(User::getEmail)
                .containsExactly("admin@example.com", "organizer@example.com", "regular@example.com");
    }

    @Test
    void countsEventsAndCategories() {
        User organizer = user("organizer@example.com", role(RoleName.ROLE_USER));
        Category concerts = category("Concerts", "concerts");
        Category meetings = category("Meetings", "meetings");
        Venue venue = venue("Palace", "Kyiv");

        eventRepository.saveAndFlush(event(organizer, concerts, venue, EventStatus.DRAFT));
        eventRepository.saveAndFlush(event(organizer, concerts, venue, EventStatus.REVIEW));
        eventRepository.saveAndFlush(event(organizer, concerts, venue, EventStatus.PUBLISHED));
        eventRepository.saveAndFlush(event(organizer, meetings, venue, EventStatus.CANCELLED));

        assertThat(eventRepository.count()).isEqualTo(4L);
        assertThat(eventRepository.countByStatus(EventStatus.DRAFT)).isEqualTo(1L);
        assertThat(eventRepository.countByStatus(EventStatus.REVIEW)).isEqualTo(1L);
        assertThat(eventRepository.countByStatus(EventStatus.PUBLISHED)).isEqualTo(1L);
        assertThat(eventRepository.countByStatus(EventStatus.CANCELLED)).isEqualTo(1L);
        assertThat(eventRepository.countByCategory_Id(concerts.getId())).isEqualTo(3L);
        assertThat(eventRepository.findTop50ByOrderByCreatedAtDesc())
                .extracting(Event::getStatus)
                .containsExactlyInAnyOrder(EventStatus.CANCELLED, EventStatus.PUBLISHED, EventStatus.REVIEW, EventStatus.DRAFT);
    }

    @Test
    void findsFailedPaymentIssues() {
        User customer = user("buyer@example.com", role(RoleName.ROLE_USER));
        User organizer = user("organizer@example.com", role(RoleName.ROLE_USER));
        Category category = category("Concerts", "concerts");
        Venue venue = venue("Palace", "Kyiv");
        Event event = eventRepository.saveAndFlush(event(organizer, category, venue, EventStatus.PUBLISHED));

        Order order = new Order(customer, event, Instant.parse("2026-07-01T10:00:00Z"));
        order.updateTotals(new BigDecimal("120.00"), "USD");
        orderRepository.saveAndFlush(order);

        Payment payment = new Payment(
                customer,
                order,
                "LOCAL_MANUAL",
                "local-ref",
                new BigDecimal("120.00"),
                "USD",
                Instant.parse("2026-07-01T10:05:00Z"));
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.FAILED,
                "LOCAL_MANUAL",
                "local-ref",
                new BigDecimal("120.00"),
                "USD",
                "Gateway timeout"));
        payment.markFailed("Gateway timeout", Instant.parse("2026-07-01T10:06:00Z"));
        paymentRepository.saveAndFlush(payment);

        assertThat(paymentRepository.countByStatus(PaymentStatus.FAILED)).isEqualTo(1L);
        assertThat(paymentRepository.findTop20ByStatusInOrderByStartedAtDesc(List.of(PaymentStatus.FAILED)))
                .extracting(Payment::getProviderReference)
                .containsExactly("local-ref");
    }

    @Test
    void returnsRecentAuditTrailFirst() throws InterruptedException {
        User actor = user("admin@example.com", role(RoleName.ROLE_USER), role(RoleName.ROLE_ADMIN));
        Category category = category("Concerts", "concerts");
        AuditLog first = auditLogRepository.saveAndFlush(new AuditLog(actor, "Category", category.getId(), "CREATE_CATEGORY", "Created"));
        Thread.sleep(100L);
        AuditLog second = auditLogRepository.saveAndFlush(new AuditLog(actor, "Category", category.getId(), "UPDATE_CATEGORY", "Updated"));

        assertThat(auditLogRepository.findTop50ByOrderByCreatedAtDesc())
                .extracting(AuditLog::getAction)
                .containsExactly("UPDATE_CATEGORY", "CREATE_CATEGORY");
    }

    @Test
    void persistsModerationActions() {
        User admin = user("admin@example.com", role(RoleName.ROLE_USER), role(RoleName.ROLE_ADMIN));
        User organizer = user("organizer@example.com", role(RoleName.ROLE_USER));
        Category category = category("Concerts", "concerts");
        Venue venue = venue("Palace", "Kyiv");
        Event event = eventRepository.saveAndFlush(event(organizer, category, venue, EventStatus.REVIEW));

        moderationActionRepository.saveAndFlush(new ModerationAction(
                admin,
                event,
                ModerationActionType.CANCEL,
                "Content policy violation"));

        assertThat(moderationActionRepository.findTop50ByOrderByCreatedAtDesc())
                .extracting(ModerationAction::getReason)
                .containsExactly("Content policy violation");
    }

    private Role role(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(roleName)));
    }

    private User user(String email, Role... roles) {
        User user = User.register(email, "{noop}secret", roles[0], new UserProfile(null, null, null, null, null));
        for (int index = 1; index < roles.length; index++) {
            user.assignRole(roles[index]);
        }
        return userRepository.saveAndFlush(user);
    }

    private Category category(String name, String slug) {
        Category category = new Category(name, slug);
        return categoryRepository.saveAndFlush(category);
    }

    private Venue venue(String name, String city) {
        Venue venue = new Venue(name, city, "Main street 1", "01001", "Ukraine");
        return venueRepository.saveAndFlush(venue);
    }

    private Event event(User organizer, Category category, Venue venue, EventStatus status) {
        Event event = new Event(
                organizer,
                category,
                venue,
                "Rock Night",
                "rock-night-" + UUID.randomUUID(),
                "Short description",
                "Full description",
                status,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                status == EventStatus.PUBLISHED ? Instant.parse("2026-08-01T10:00:00Z") : null);
        if (status != EventStatus.CANCELLED) {
            event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        }
        return event;
    }
}
