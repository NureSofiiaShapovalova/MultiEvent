package nure.com.eventflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import nure.com.eventflow.ticketing.application.GetPublicTicketOptionsService;
import nure.com.eventflow.ticketing.application.TicketOptionView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPublishedEventDetailsServiceTest {

    @Mock
    private EventCatalogQueryRepository eventCatalogQueryRepository;

    @Mock
    private CatalogViewMapper catalogViewMapper;

    @Mock
    private GetPublicTicketOptionsService getPublicTicketOptionsService;

    @InjectMocks
    private GetPublishedEventDetailsService getPublishedEventDetailsService;

    @Test
    void mapsEventToDetailsView() {
        Event event = sampleEvent();
        EventDetailsView detailsView = new EventDetailsView(
                UUID.randomUUID(),
                "Rock Night",
                "rock-night",
                "Short",
                "Full",
                "Concerts",
                "Palace",
                "Kyiv",
                "Main street 1",
                "01001",
                "Ukraine",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                "organizer@example.com",
                "https://example.com/cover.jpg",
                List.of("https://example.com/cover.jpg"),
                List.of(new TicketOptionView(UUID.randomUUID(), "Standard", "Entry ticket", java.math.BigDecimal.valueOf(25), "USD", 4, 20, true, "On sale")));
        when(eventCatalogQueryRepository.findPublishedEventBySlug("rock-night")).thenReturn(Optional.of(event));
        when(getPublicTicketOptionsService.getByEventSlug("rock-night")).thenReturn(detailsView.ticketOptions());
        when(catalogViewMapper.toDetailsView(event, detailsView.ticketOptions())).thenReturn(detailsView);

        assertThat(getPublishedEventDetailsService.getBySlug("rock-night")).isEqualTo(detailsView);
    }

    private Event sampleEvent() {
        User organizer = User.register("organizer@example.com", "{noop}secret", new Role(RoleName.ROLE_USER), new UserProfile(null, null, null, null, null));
        Category category = new Category("Concerts", "concerts");
        Venue venue = new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine");
        return new Event(organizer, category, venue, "Rock Night", "rock-night", "Short", "Full", EventStatus.PUBLISHED, Instant.now(), Instant.now(), Instant.now());
    }
}
