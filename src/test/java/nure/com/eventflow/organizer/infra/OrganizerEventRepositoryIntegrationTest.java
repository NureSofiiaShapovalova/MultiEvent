package nure.com.eventflow.organizer.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.VenueRepository;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.OrganizerTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class OrganizerEventRepositoryIntegrationTest {

    @Autowired
    private OrganizerEventRepository organizerEventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void returnsOnlyOwnedEvents() {
        User organizer = organizer("organizer@example.com");
        User otherOrganizer = organizer("other@example.com");
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));

        organizerEventRepository.saveAndFlush(event(organizer, category, venue, "Jazz Night", "jazz-night", EventStatus.DRAFT));
        organizerEventRepository.saveAndFlush(event(organizer, category, venue, "Rock Night", "rock-night", EventStatus.PUBLISHED));
        organizerEventRepository.saveAndFlush(event(otherOrganizer, category, venue, "Other Night", "other-night", EventStatus.DRAFT));

        assertThat(organizerEventRepository.findByOrganizer_IdOrderByCreatedAtDesc(organizer.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
                .extracting(Event::getSlug)
                .containsExactlyInAnyOrder("jazz-night", "rock-night");
        assertThat(organizerEventRepository.findByIdAndOrganizer_Id(organizerEventRepository.findBySlug("rock-night").orElseThrow().getId(), organizer.getId()))
                .isPresent();
        assertThat(organizerEventRepository.findByIdAndOrganizer_Id(organizerEventRepository.findBySlug("other-night").orElseThrow().getId(), organizer.getId()))
                .isEmpty();
    }

    private User organizer(String email) {
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER)));
        return userRepository.saveAndFlush(User.register(email, "{noop}secret", role, new UserProfile(null, null, null, null, null)));
    }

    private Event event(User organizer, Category category, Venue venue, String title, String slug, EventStatus status) {
        Event event = new Event(
                organizer,
                category,
                venue,
                title,
                slug,
                "Short description",
                "Full description",
                status,
                java.time.Instant.parse("2026-09-01T18:00:00Z"),
                java.time.Instant.parse("2026-09-01T21:00:00Z"),
                status == EventStatus.PUBLISHED ? java.time.Instant.parse("2026-08-01T10:00:00Z") : null);
        event.replaceImages(List.of(new nure.com.eventflow.catalog.domain.EventImage(event, "https://example.com/cover.jpg", true, 0)));
        return event;
    }
}
