package nure.com.eventflow.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

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

    @InjectMocks
    private GetAdminDashboardService getAdminDashboardService;

    @Test
    void returnsSummaryCounts() {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByRoleName(RoleName.ROLE_ORGANIZER)).thenReturn(3L);
        when(categoryRepository.count()).thenReturn(5L);
        when(eventRepository.count()).thenReturn(18L);
        when(eventRepository.countByStatus(EventStatus.DRAFT)).thenReturn(4L);
        when(eventRepository.countByStatus(EventStatus.REVIEW)).thenReturn(2L);
        when(eventRepository.countByStatus(EventStatus.PUBLISHED)).thenReturn(9L);
        when(eventRepository.countByStatus(EventStatus.CANCELLED)).thenReturn(1L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(2L);
        when(auditLogRepository.count()).thenReturn(44L);

        AdminDashboardView view = getAdminDashboardService.get();

        assertThat(view.totalUsers()).isEqualTo(12L);
        assertThat(view.organizers()).isEqualTo(3L);
        assertThat(view.categories()).isEqualTo(5L);
        assertThat(view.events()).isEqualTo(18L);
        assertThat(view.draftEvents()).isEqualTo(4L);
        assertThat(view.reviewEvents()).isEqualTo(2L);
        assertThat(view.publishedEvents()).isEqualTo(9L);
        assertThat(view.cancelledEvents()).isEqualTo(1L);
        assertThat(view.failedPayments()).isEqualTo(2L);
        assertThat(view.auditEntries()).isEqualTo(44L);
    }
}
