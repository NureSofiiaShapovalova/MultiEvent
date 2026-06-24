package nure.com.eventflow.organizer.infra;

import java.util.List;
import java.util.UUID;
import nure.com.eventflow.organizer.domain.EventPublishHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventPublishHistoryRepository extends JpaRepository<EventPublishHistory, UUID> {

    List<EventPublishHistory> findByEvent_IdOrderByCreatedAtDesc(UUID eventId);
}
