package nure.com.eventflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.EventCatalogQueryRepository;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SearchPublishedEventsServiceTest {

    @Mock
    private EventCatalogQueryRepository eventCatalogQueryRepository;

    @Mock
    private CatalogViewMapper catalogViewMapper;

    @InjectMocks
    private SearchPublishedEventsService searchPublishedEventsService;

    @Test
    void mapsPageOfEventsToCardViews() {
        Event event = sampleEvent();
        EventCardView cardView = new EventCardView(UUID.randomUUID(), "Rock Night", "rock-night", "Short", "Concerts", "Kyiv", Instant.now(), Instant.now(), "https://example.com/cover.jpg");
        when(eventCatalogQueryRepository.searchPublishedEvents(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(catalogViewMapper.toCardView(event)).thenReturn(cardView);

        Page<EventCardView> result = searchPublishedEventsService.search(new EventSearchCriteria(null, null, null, null, null, "startsAtAsc"), PageRequest.of(0, 12));

        assertThat(result.getContent()).containsExactly(cardView);
    }

    private Event sampleEvent() {
        User organizer = User.register("organizer@example.com", "{noop}secret", new Role(RoleName.ROLE_USER), new UserProfile(null, null, null, null, null));
        Category category = new Category("Concerts", "concerts");
        Venue venue = new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine");
        return new Event(organizer, category, venue, "Rock Night", "rock-night", "Short", "Full", EventStatus.PUBLISHED, Instant.now(), Instant.now(), Instant.now());
    }
}
