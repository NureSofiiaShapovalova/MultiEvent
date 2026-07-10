package nure.com.multievent.ticketing.infra;

import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.ticketing.domain.TicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketInventoryRepository extends JpaRepository<TicketInventory, UUID> {

    Optional<TicketInventory> findByTicketType_Id(UUID ticketTypeId);
}
