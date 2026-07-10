package nure.com.multievent.catalog.application;

import java.util.List;
import java.time.Clock;
import java.time.Instant;
import nure.com.multievent.catalog.infra.EventCatalogQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetHomepageSectionsService {

    private static final int DEFAULT_SECTION_SIZE = 6;

    private final EventCatalogQueryRepository eventCatalogQueryRepository;
    private final CatalogViewMapper catalogViewMapper;
    private final Clock clock;

    public GetHomepageSectionsService(
            EventCatalogQueryRepository eventCatalogQueryRepository,
            CatalogViewMapper catalogViewMapper,
            Clock clock
    ) {
        this.eventCatalogQueryRepository = eventCatalogQueryRepository;
        this.catalogViewMapper = catalogViewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HomepageView getHomepage() {
        Instant now = clock.instant();
        List<EventCardView> upcomingEvents = eventCatalogQueryRepository.findUpcomingPublishedEvents(now, DEFAULT_SECTION_SIZE)
                .stream()
                .map(catalogViewMapper::toCardView)
                .toList();
        List<EventCardView> newestEvents = eventCatalogQueryRepository.findNewestPublishedEvents(DEFAULT_SECTION_SIZE)
                .stream()
                .map(catalogViewMapper::toCardView)
                .toList();
        return new HomepageView(upcomingEvents, newestEvents);
    }
}
