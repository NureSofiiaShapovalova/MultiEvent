package nure.com.multievent.catalog.application;

import nure.com.multievent.catalog.infra.EventCatalogQueryRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import nure.com.multievent.ticketing.application.GetPublicTicketOptionsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPublishedEventDetailsService {

    private final EventCatalogQueryRepository eventCatalogQueryRepository;
    private final CatalogViewMapper catalogViewMapper;
    private final GetPublicTicketOptionsService getPublicTicketOptionsService;

    public GetPublishedEventDetailsService(
            EventCatalogQueryRepository eventCatalogQueryRepository,
            CatalogViewMapper catalogViewMapper,
            GetPublicTicketOptionsService getPublicTicketOptionsService
    ) {
        this.eventCatalogQueryRepository = eventCatalogQueryRepository;
        this.catalogViewMapper = catalogViewMapper;
        this.getPublicTicketOptionsService = getPublicTicketOptionsService;
    }

    @Transactional(readOnly = true)
    public EventDetailsView getBySlug(String slug) {
        return eventCatalogQueryRepository.findPublishedEventBySlug(slug)
                .map(event -> catalogViewMapper.toDetailsView(event, getPublicTicketOptionsService.getByEventSlug(slug)))
                .orElseThrow(() -> new NotFoundException("Event was not found."));
    }
}
