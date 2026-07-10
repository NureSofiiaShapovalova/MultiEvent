package nure.com.multievent.checkout.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.multievent.checkout.domain.TicketReservation;
import nure.com.multievent.checkout.domain.TicketReservationPolicy;
import nure.com.multievent.checkout.infra.TicketReservationRepository;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdjustTicketReservationService {

    private final UserRepository userRepository;
    private final TicketReservationRepository ticketReservationRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final TicketReservationPolicy ticketReservationPolicy;
    private final Clock clock;

    public AdjustTicketReservationService(
            UserRepository userRepository,
            TicketReservationRepository ticketReservationRepository,
            TicketInventoryRepository ticketInventoryRepository,
            TicketReservationPolicy ticketReservationPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.ticketReservationRepository = ticketReservationRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
        this.ticketReservationPolicy = ticketReservationPolicy;
        this.clock = clock;
    }

    @Transactional
    public void adjust(String email, UUID reservationId, AdjustReservationCommand command) {
        User customer = findCustomer(email);
        TicketReservation reservation = ticketReservationRepository.findByIdAndCustomer_Id(reservationId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Reservation was not found."));

        TicketType ticketType = reservation.getPrimaryItem().getTicketType();
        TicketInventory inventory = ticketType.getInventory();
        ticketReservationPolicy.assertCanAdjustReservation(reservation, inventory, command.quantity(), clock.instant());

        int currentQuantity = reservation.getTotalQuantity();
        int delta = command.quantity() - currentQuantity;
        if (delta > 0) {
            inventory.reserve(delta);
        } else if (delta < 0) {
            inventory.release(Math.abs(delta));
        }
        reservation.changeQuantity(command.quantity());
        ticketReservationRepository.save(reservation);
        ticketInventoryRepository.save(inventory);
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
