package nure.com.multievent.checkout.infra;

import java.util.UUID;
import nure.com.multievent.checkout.domain.TicketReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketReservationItemRepository extends JpaRepository<TicketReservationItem, UUID> {
}
