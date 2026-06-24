package nure.com.eventflow.catalog.application;

import nure.com.eventflow.catalog.infra.EventCatalogQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchPublishedEventsService {

    private final EventCatalogQueryRepository eventCatalogQueryRepository;
    private final CatalogViewMapper catalogViewMapper;

    public SearchPublishedEventsService(
            EventCatalogQueryRepository eventCatalogQueryRepository,
            CatalogViewMapper catalogViewMapper
    ) {
        this.eventCatalogQueryRepository = eventCatalogQueryRepository;
        this.catalogViewMapper = catalogViewMapper;
    }

    @Transactional(readOnly = true)
    public Page<EventCardView> search(EventSearchCriteria criteria, Pageable pageable) {
        return eventCatalogQueryRepository.searchPublishedEvents(criteria, pageable)
                .map(catalogViewMapper::toCardView);
    }
}
