package nure.com.eventflow.ticketing.application;

import java.time.Clock;
import java.util.List;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPublicTicketOptionsService {

    private final TicketingQueryRepository ticketingQueryRepository;
    private final TicketingViewMapper ticketingViewMapper;
    private final Clock clock;

    public GetPublicTicketOptionsService(
            TicketingQueryRepository ticketingQueryRepository,
            TicketingViewMapper ticketingViewMapper,
            Clock clock
    ) {
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.ticketingViewMapper = ticketingViewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TicketOptionView> getByEventSlug(String slug) {
        var now = clock.instant();
        return ticketingQueryRepository.findPublicSellableTicketTypesByEventSlug(slug, now)
                .stream()
                .map(ticketType -> ticketingViewMapper.toTicketOptionView(ticketType, now))
                .toList();
    }
}
