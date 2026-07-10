package nure.com.multievent.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.domain.Venue;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.iam.infra.RoleRepository;
import nure.com.multievent.iam.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class EventRepositoryIntegrationTest {

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
    void findsPublishedEventBySlugWithRelations() {
        User organizer = organizer();
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));

        Event event = eventRepository.saveAndFlush(new Event(
                organizer,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "A night of live rock music.",
                "Full details",
                EventStatus.PUBLISHED,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z")));

        eventImageRepository.saveAndFlush(new EventImage(event, "https://example.com/cover.jpg", true, 0));
        eventImageRepository.saveAndFlush(new EventImage(event, "https://example.com/gallery-1.jpg", false, 1));
        entityManager.clear();

        assertThat(eventRepository.findBySlugAndStatus("rock-night", EventStatus.PUBLISHED))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getCategory().getName()).isEqualTo("Concerts");
                    assertThat(found.getVenue().getCity()).isEqualTo("Kyiv");
                    assertThat(found.getImages()).hasSize(2);
                });
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
