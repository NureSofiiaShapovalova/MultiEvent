package nure.com.eventflow.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import nure.com.eventflow.catalog.application.EventSearchCriteria;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import(EventCatalogQueryRepository.class)
class EventCatalogQueryRepositoryIntegrationTest {

    @Autowired
    private EventCatalogQueryRepository eventCatalogQueryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventImageRepository eventImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void searchesOnlyPublishedEvents() {
        User organizer = organizer();
        Category concerts = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue kyiv = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));
        Venue lviv = venueRepository.saveAndFlush(new Venue("Hall", "Lviv", "Freedom avenue 2", "79000", "Ukraine"));

        eventRepository.saveAndFlush(publishedEvent(organizer, "Rock Night", "rock-night", concerts, kyiv, "Rock is back", Instant.parse("2026-09-01T18:00:00Z")));
        eventRepository.saveAndFlush(publishedEvent(organizer, "Jazz Night", "jazz-night", concerts, lviv, "Smooth jazz", Instant.parse("2026-09-05T18:00:00Z")));
        eventRepository.saveAndFlush(draftEvent(organizer, "Draft Only", "draft-only", concerts, kyiv));

        var page = eventCatalogQueryRepository.searchPublishedEvents(
                new EventSearchCriteria("rock", null, "Kyiv", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), "startsAtAsc"),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Event::getSlug).containsExactly("rock-night");
    }

    @Test
    void findsPublishedEventBySlug() {
        User organizer = organizer();
        Category concerts = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));

        Event event = eventRepository.saveAndFlush(publishedEvent(organizer, "Rock Night", "rock-night", concerts, venue, "Rock is back", Instant.parse("2026-09-01T18:00:00Z")));
        eventImageRepository.saveAndFlush(new EventImage(event, "https://example.com/cover.jpg", true, 0));
        entityManager.clear();

        assertThat(eventCatalogQueryRepository.findPublishedEventBySlug("rock-night"))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getCategory().getSlug()).isEqualTo("concerts");
                    assertThat(found.getVenue().getCity()).isEqualTo("Kyiv");
                    assertThat(found.getImages()).hasSize(1);
                });
    }

    private Event publishedEvent(User organizer, String title, String slug, Category category, Venue venue, String description, Instant startsAt) {
        return new Event(organizer, category, venue, title, slug, description, "Full details", EventStatus.PUBLISHED, startsAt, startsAt.plusSeconds(10_800), startsAt.minusSeconds(86_400));
    }

    private Event draftEvent(User organizer, String title, String slug, Category category, Venue venue) {
        Instant startsAt = Instant.parse("2026-10-01T18:00:00Z");
        return new Event(organizer, category, venue, title, slug, "Draft description", "Draft full description", EventStatus.DRAFT, startsAt, startsAt.plusSeconds(10_800), null);
    }

    private User organizer() {
        Role role = roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER));
        return userRepository.saveAndFlush(User.register(
                "organizer@example.com",
                "{noop}secret",
                role,
                new UserProfile(null, null, null, null, null)));
    }
}
