package nure.com.eventflow.ticketing.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
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
import nure.com.eventflow.ticketing.domain.TicketType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TicketTypeRepositoryIntegrationTest {

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private TicketInventoryRepository ticketInventoryRepository;

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
    void savesTicketTypeWithInventoryAndFindsOwnedTicketType() {
        User organizer = organizer("organizer@example.com");
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));
        Event event = new Event(
                organizer,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                EventStatus.DRAFT,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                null);
        event.replaceImages(List.of(new nure.com.eventflow.catalog.domain.EventImage(event, "https://example.com/cover.jpg", true, 0)));
        entityManager.persist(event);
        entityManager.flush();

        TicketType ticketType = new TicketType(
                event,
                "Standard",
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                true);
        ticketType.attachInventory(new nure.com.eventflow.ticketing.domain.TicketInventory(ticketType, 100));

        ticketTypeRepository.saveAndFlush(ticketType);

        assertThat(ticketTypeRepository.findByIdAndEvent_Organizer_Id(ticketType.getId(), organizer.getId())).isPresent();
        assertThat(ticketInventoryRepository.findByTicketType_Id(ticketType.getId())).isPresent();
    }

    private User organizer(String email) {
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER)));
        return userRepository.saveAndFlush(User.register(email, "{noop}secret", role, new UserProfile(null, null, null, null, null)));
    }
}
