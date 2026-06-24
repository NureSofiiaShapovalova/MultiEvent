package nure.com.eventflow.admin.application;

import java.util.List;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAdminDashboardService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;

    public GetAdminDashboardService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            EventRepository eventRepository,
            PaymentRepository paymentRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardView get() {
        return new AdminDashboardView(
                userRepository.count(),
                userRepository.countByRoleName(RoleName.ROLE_ORGANIZER),
                categoryRepository.count(),
                eventRepository.count(),
                eventRepository.countByStatus(EventStatus.DRAFT),
                eventRepository.countByStatus(EventStatus.REVIEW),
                eventRepository.countByStatus(EventStatus.PUBLISHED),
                eventRepository.countByStatus(EventStatus.CANCELLED),
                paymentRepository.countByStatus(PaymentStatus.FAILED),
                auditLogRepository.count());
    }
}
