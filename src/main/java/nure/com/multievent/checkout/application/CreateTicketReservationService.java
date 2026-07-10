package nure.com.multievent.checkout.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.checkout.domain.TicketReservation;
import nure.com.multievent.checkout.domain.TicketReservationItem;
import nure.com.multievent.checkout.domain.TicketReservationPolicy;
import nure.com.multievent.checkout.infra.TicketReservationRepository;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTicketReservationService {

    private static final Duration RESERVATION_HOLD_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final TicketingQueryRepository ticketingQueryRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final TicketReservationRepository ticketReservationRepository;
    private final TicketReservationPolicy ticketReservationPolicy;
    private final Clock clock;

    public CreateTicketReservationService(
            UserRepository userRepository,
            TicketingQueryRepository ticketingQueryRepository,
            TicketInventoryRepository ticketInventoryRepository,
            TicketReservationRepository ticketReservationRepository,
            TicketReservationPolicy ticketReservationPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
        this.ticketReservationRepository = ticketReservationRepository;
        this.ticketReservationPolicy = ticketReservationPolicy;
        this.clock = clock;
    }

    @Transactional
    public UUID create(String email, String slug, CreateReservationCommand command) {
        User customer = findCustomer(email);
        Instant now = clock.instant();
        TicketType ticketType = ticketingQueryRepository.findPublicSellableTicketTypeBySlugAndId(slug, command.ticketTypeId(), now)
                .orElseThrow(() -> new NotFoundException("Ticket type was not found."));

        ticketReservationPolicy.assertCanCreateReservation(ticketType, ticketType.getInventory(), command.quantity(), now);
        ticketReservationPolicy.assertExpiryOrdering(now.plus(RESERVATION_HOLD_DURATION), now);

        TicketReservation reservation = new TicketReservation(customer, ticketType.getEvent(), now.plus(RESERVATION_HOLD_DURATION));
        reservation.addItem(new TicketReservationItem(ticketType, command.quantity()));
        ticketType.getInventory().reserve(command.quantity());

        ticketReservationRepository.save(reservation);
        ticketInventoryRepository.save(ticketType.getInventory());
        return reservation.getId();
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
