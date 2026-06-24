package nure.com.eventflow.ticketing.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
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
import nure.com.eventflow.ticketing.domain.TicketType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import(TicketingQueryRepository.class)
class TicketingQueryRepositoryIntegrationTest {

    @Autowired
    private TicketingQueryRepository ticketingQueryRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsOnlyOwnedOrganizerTicketTypesAndActivePublicOptions() {
        User organizer = organizer("organizer@example.com");
        User otherOrganizer = organizer("other@example.com");
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));

        Event organizerEvent = event(organizer, category, venue, "rock-night", EventStatus.PUBLISHED);
        Event otherEvent = event(otherOrganizer, category, venue, "other-night", EventStatus.PUBLISHED);

        TicketType active = ticketType(organizerEvent, "Standard", true, 100);
        TicketType inactive = ticketType(organizerEvent, "VIP", false, 50);
        TicketType otherTicketType = ticketType(otherEvent, "Other", true, 30);

        ticketTypeRepository.saveAllAndFlush(List.of(active, inactive, otherTicketType));

        assertThat(ticketingQueryRepository.findOrganizerTicketTypes(organizer.getId(), organizerEvent.getId()))
                .extracting(TicketType::getName)
                .containsExactlyInAnyOrder("VIP", "Standard");
        assertThat(ticketingQueryRepository.findOrganizerTicketTypes(organizer.getId(), otherEvent.getId()))
                .isEmpty();
        assertThat(ticketingQueryRepository.findOrganizerTicketTypes(otherOrganizer.getId(), otherEvent.getId()))
                .extracting(TicketType::getName)
                .containsExactly("Other");
        assertThat(ticketingQueryRepository.findPublicSellableTicketTypesByEventSlug("rock-night", Instant.parse("2026-08-15T00:00:00Z")))
                .extracting(TicketType::getName)
                .containsExactly("Standard");
    }

    private User organizer(String email) {
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER)));
        return userRepository.saveAndFlush(User.register(email, "{noop}secret", role, new UserProfile(null, null, null, null, null)));
    }

    private Event event(User organizer, Category category, Venue venue, String slug, EventStatus status) {
        Event event = new Event(
                organizer,
                category,
                venue,
                "Rock Night",
                slug,
                "Short description",
                "Full description",
                status,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                status == EventStatus.PUBLISHED ? Instant.parse("2026-08-01T10:00:00Z") : null);
        event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        entityManager.persist(event);
        entityManager.flush();
        return event;
    }

    private TicketType ticketType(Event event, String name, boolean active, int totalQuantity) {
        TicketType ticketType = new TicketType(
                event,
                name,
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                active);
        ticketType.attachInventory(new nure.com.eventflow.ticketing.domain.TicketInventory(ticketType, totalQuantity));
        return ticketType;
    }
}
