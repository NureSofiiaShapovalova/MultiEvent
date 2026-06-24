package nure.com.eventflow.checkout.infra;

import java.util.UUID;
import nure.com.eventflow.checkout.domain.ReservationLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationLockRepository extends JpaRepository<ReservationLock, UUID> {
}
