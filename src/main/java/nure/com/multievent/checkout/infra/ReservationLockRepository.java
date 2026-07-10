package nure.com.multievent.checkout.infra;

import java.util.UUID;
import nure.com.multievent.checkout.domain.ReservationLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationLockRepository extends JpaRepository<ReservationLock, UUID> {
}
