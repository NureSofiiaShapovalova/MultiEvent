package nure.com.eventflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class GetHomepageSectionsServiceTest {

    @Mock
    private EventCatalogQueryRepository eventCatalogQueryRepository;

    @Mock
    private CatalogViewMapper catalogViewMapper;

    @Mock
    private Clock clock;

    @InjectMocks
    private GetHomepageSectionsService getHomepageSectionsService;

    @Test
    void returnsUpcomingAndNewestSections() {
        Instant now = Instant.parse("2026-06-22T16:00:00Z");
        when(clock.instant()).thenReturn(now);

        Event upcoming = sampleEvent("Upcoming", "upcoming");
        Event newest = sampleEvent("Newest", "newest");
        EventCardView upcomingView = new EventCardView(null, "Upcoming", "upcoming", "Short", "Concerts", "Kyiv", now, now, null);
        EventCardView newestView = new EventCardView(null, "Newest", "newest", "Short", "Concerts", "Kyiv", now, now, null);

        when(eventCatalogQueryRepository.findUpcomingPublishedEvents(now, 6)).thenReturn(List.of(upcoming));
        when(eventCatalogQueryRepository.findNewestPublishedEvents(6)).thenReturn(List.of(newest));
        when(catalogViewMapper.toCardView(upcoming)).thenReturn(upcomingView);
        when(catalogViewMapper.toCardView(newest)).thenReturn(newestView);

        HomepageView homepageView = getHomepageSectionsService.getHomepage();

        assertThat(homepageView.upcomingEvents()).containsExactly(upcomingView);
        assertThat(homepageView.newestEvents()).containsExactly(newestView);
    }

    private Event sampleEvent(String title, String slug) {
        return new Event(
                User.register(
                        "organizer@example.com",
                        "{noop}secret",
                        new Role(RoleName.ROLE_USER),
                        new UserProfile(null, null, null, null, null)),
                new Category("Concerts", "concerts"),
                new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"),
                title,
                slug,
                "Short",
                "Full",
                EventStatus.PUBLISHED,
                Instant.now(),
                Instant.now(),
                Instant.now());
    }
}
