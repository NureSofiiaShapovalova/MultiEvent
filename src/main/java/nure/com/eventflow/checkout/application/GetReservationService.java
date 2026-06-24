package nure.com.eventflow.checkout.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.eventflow.checkout.infra.TicketReservationRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReservationService {

    private final UserRepository userRepository;
    private final TicketReservationRepository ticketReservationRepository;
    private final CheckoutViewMapper checkoutViewMapper;
    private final Clock clock;

    public GetReservationService(
            UserRepository userRepository,
            TicketReservationRepository ticketReservationRepository,
            CheckoutViewMapper checkoutViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.ticketReservationRepository = ticketReservationRepository;
        this.checkoutViewMapper = checkoutViewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReservationView getById(String email, UUID reservationId) {
        User customer = findCustomer(email);
        return ticketReservationRepository.findByIdAndCustomer_Id(reservationId, customer.getId())
                .map(reservation -> checkoutViewMapper.toReservationView(reservation, clock.instant()))
                .orElseThrow(() -> new NotFoundException("Reservation was not found."));
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
