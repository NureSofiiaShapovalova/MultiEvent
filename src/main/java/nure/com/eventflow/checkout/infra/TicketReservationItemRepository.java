package nure.com.eventflow.checkout.infra;

import java.util.UUID;
import nure.com.eventflow.checkout.domain.TicketReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketReservationItemRepository extends JpaRepository<TicketReservationItem, UUID> {
}
