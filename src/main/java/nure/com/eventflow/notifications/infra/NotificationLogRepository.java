package nure.com.eventflow.notifications.infra;

import java.util.List;
import java.util.UUID;
import nure.com.eventflow.notifications.domain.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationRecord, UUID> {

    List<NotificationRecord> findByRecipient_IdOrderByCreatedAtDesc(UUID recipientId);
}
