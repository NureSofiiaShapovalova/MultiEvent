package nure.com.eventflow.checkout.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.eventflow.checkout.domain.TicketReservation;
import nure.com.eventflow.checkout.domain.TicketReservationPolicy;
import nure.com.eventflow.checkout.infra.TicketReservationRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.infra.TicketInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmTicketReservationService {

    private final UserRepository userRepository;
    private final TicketReservationRepository ticketReservationRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final TicketReservationPolicy ticketReservationPolicy;
    private final Clock clock;

    public ConfirmTicketReservationService(
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

    @Transactional(noRollbackFor = BusinessRuleViolationException.class)
    public void confirm(String email, UUID reservationId) {
        User customer = findCustomer(email);
        TicketReservation reservation = ticketReservationRepository.findByIdAndCustomer_Id(reservationId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Reservation was not found."));

        if (!clock.instant().isBefore(reservation.getExpiresAt())) {
            expire(reservation);
            throw new BusinessRuleViolationException("Reservation has expired.");
        }

        ticketReservationPolicy.assertCanConfirm(reservation, clock.instant());
        TicketInventory inventory = reservation.getPrimaryItem().getTicketType().getInventory();
        inventory.markSold(reservation.getTotalQuantity());
        reservation.confirm(clock.instant());
        ticketReservationRepository.save(reservation);
        ticketInventoryRepository.save(inventory);
    }

    private void expire(TicketReservation reservation) {
        TicketInventory inventory = reservation.getPrimaryItem().getTicketType().getInventory();
        inventory.release(reservation.getTotalQuantity());
        reservation.expire(clock.instant());
        ticketReservationRepository.save(reservation);
        ticketInventoryRepository.save(inventory);
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
