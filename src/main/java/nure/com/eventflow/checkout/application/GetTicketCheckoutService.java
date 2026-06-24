package nure.com.eventflow.checkout.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.eventflow.shared.exception.NotFoundException;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTicketCheckoutService {

    private final TicketingQueryRepository ticketingQueryRepository;
    private final CheckoutViewMapper checkoutViewMapper;
    private final Clock clock;

    public GetTicketCheckoutService(
            TicketingQueryRepository ticketingQueryRepository,
            CheckoutViewMapper checkoutViewMapper,
            Clock clock
    ) {
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.checkoutViewMapper = checkoutViewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TicketCheckoutView getBySlugAndTicketTypeId(String slug, UUID ticketTypeId) {
        var now = clock.instant();
        return ticketingQueryRepository.findPublicSellableTicketTypeBySlugAndId(slug, ticketTypeId, now)
                .map(ticketType -> checkoutViewMapper.toCheckoutView(ticketType, now))
                .orElseThrow(() -> new NotFoundException("Ticket type was not found."));
    }
}
