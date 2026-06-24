package nure.com.eventflow.organizer.infra;

import java.util.List;
import java.util.UUID;
import nure.com.eventflow.organizer.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findTop50ByOrderByCreatedAtDesc();
}
