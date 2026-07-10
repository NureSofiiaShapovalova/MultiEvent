package nure.com.multievent.ticketing.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.ticketing.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    Optional<TicketType> findByIdAndEvent_Organizer_Id(UUID ticketTypeId, UUID organizerId);

    List<TicketType> findByEvent_IdOrderByCreatedAtAsc(UUID eventId);
}
