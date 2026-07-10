package nure.com.multievent.notifications.infra;

import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.notifications.domain.EventSubscriptionPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPreferenceRepository extends JpaRepository<EventSubscriptionPreference, UUID> {

    Optional<EventSubscriptionPreference> findByUser_Id(UUID userId);
}
