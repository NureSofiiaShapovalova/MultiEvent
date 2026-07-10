package nure.com.multievent.admin.application;

import java.util.List;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.catalog.infra.EventRepository;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
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
