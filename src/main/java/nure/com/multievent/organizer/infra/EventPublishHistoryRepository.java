package nure.com.multievent.organizer.infra;

import java.util.List;
import java.util.UUID;
import nure.com.multievent.organizer.domain.EventPublishHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventPublishHistoryRepository extends JpaRepository<EventPublishHistory, UUID> {

    List<EventPublishHistory> findByEvent_IdOrderByCreatedAtDesc(UUID eventId);
}
