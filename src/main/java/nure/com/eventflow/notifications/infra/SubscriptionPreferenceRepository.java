package nure.com.eventflow.notifications.infra;

import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.notifications.domain.EventSubscriptionPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPreferenceRepository extends JpaRepository<EventSubscriptionPreference, UUID> {

    Optional<EventSubscriptionPreference> findByUser_Id(UUID userId);
}
