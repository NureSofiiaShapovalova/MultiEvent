package nure.com.eventflow.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.domain.OrderStatus;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentAttempt;
import nure.com.eventflow.payment.domain.PaymentAttemptType;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminListServicesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private AdminViewMapper adminViewMapper;
    private ListAdminUsersService listAdminUsersService;
    private ListAdminCategoriesService listAdminCategoriesService;
    private ListAdminEventsService listAdminEventsService;
    private ListPaymentIssuesService listPaymentIssuesService;
    private ListAuditTrailService listAuditTrailService;

    @BeforeEach
    void setUp() {
        adminViewMapper = new AdminViewMapper();
        listAdminUsersService = new ListAdminUsersService(userRepository, adminViewMapper);
        listAdminCategoriesService = new ListAdminCategoriesService(categoryRepository, eventRepository, adminViewMapper);
        listAdminEventsService = new ListAdminEventsService(eventRepository, adminViewMapper);
        listPaymentIssuesService = new ListPaymentIssuesService(paymentRepository, adminViewMapper);
        listAuditTrailService = new ListAuditTrailService(auditLogRepository, adminViewMapper);
    }

    @Test
    void listsUsersCategoriesEventsPaymentsAndAuditEntries() {
        User organizer = TestEntityFactory.user("organizer@example.com");
        organizer.assignRole(new Role(RoleName.ROLE_ORGANIZER));
        ReflectionTestUtils.setField(organizer, "id", UUID.randomUUID());
        User regular = TestEntityFactory.user("regular@example.com");
        ReflectionTestUtils.setField(regular, "id", UUID.randomUUID());
        Category category = TestEntityFactory.category("Concerts", "concerts");
        Venue venue = TestEntityFactory.venue("Palace", "Kyiv");
        Event event = TestEntityFactory.event(organizer, category, venue, EventStatus.PUBLISHED);
        Order order = new Order(regular, event, Instant.parse("2026-07-01T10:00:00Z"));
        order.updateTotals(new BigDecimal("120.00"), "USD");
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        Payment payment = new Payment(
                regular,
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
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        AuditLog auditLog = new AuditLog(organizer, "Event", event.getId(), "PUBLISH_EVENT", "Published event");
        ReflectionTestUtils.setField(auditLog, "id", UUID.randomUUID());

        when(userRepository.findAllByOrderByEmailAsc()).thenReturn(List.of(regular, organizer));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));
        when(eventRepository.countByCategory_Id(category.getId())).thenReturn(1L);
        when(eventRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(event));
        when(paymentRepository.findTop20ByStatusInOrderByStartedAtDesc(List.of(PaymentStatus.FAILED))).thenReturn(List.of(payment));
        when(auditLogRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(auditLog));

        assertThat(listAdminUsersService.list()).hasSize(2);
        assertThat(listAdminCategoriesService.list()).hasSize(1);
        assertThat(listAdminEventsService.list()).hasSize(1);
        assertThat(listPaymentIssuesService.list()).hasSize(1);
        assertThat(listAuditTrailService.list()).hasSize(1);
    }
}
