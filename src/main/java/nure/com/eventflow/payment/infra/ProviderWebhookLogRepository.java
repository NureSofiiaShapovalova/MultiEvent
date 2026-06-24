package nure.com.eventflow.payment.infra;

import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.payment.domain.ProviderWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderWebhookLogRepository extends JpaRepository<ProviderWebhookLog, UUID> {

    Optional<ProviderWebhookLog> findByProviderNameAndEventId(String providerName, String eventId);
}
