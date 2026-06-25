package nure.com.eventflow.payment.infra;

import java.util.List;
import java.util.UUID;
import nure.com.eventflow.payment.domain.RefundRequest;
import nure.com.eventflow.payment.domain.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    boolean existsByOrder_IdAndStatus(UUID orderId, RefundStatus status);

    List<RefundRequest> findByOrder_IdOrderByRequestedAtDesc(UUID orderId);
}
