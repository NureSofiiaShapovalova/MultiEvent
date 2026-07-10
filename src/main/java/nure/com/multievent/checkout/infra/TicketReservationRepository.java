package nure.com.multievent.checkout.infra;

import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.checkout.domain.TicketReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, UUID> {

    @EntityGraph(attributePaths = {"items", "items.ticketType", "event", "customer"})
    Optional<TicketReservation> findByIdAndCustomer_Id(UUID reservationId, UUID customerId);
}
