package nure.com.eventflow.admin.application;

import java.util.List;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAuditTrailService {

    private final AuditLogRepository auditLogRepository;
    private final AdminViewMapper adminViewMapper;

    public ListAuditTrailService(AuditLogRepository auditLogRepository, AdminViewMapper adminViewMapper) {
        this.auditLogRepository = auditLogRepository;
        this.adminViewMapper = adminViewMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogRowView> list() {
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(adminViewMapper::toAuditLogRowView)
                .toList();
    }
}
