package nure.com.multievent.checkout.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.multievent.checkout.CheckoutTestFixtures;
import nure.com.multievent.checkout.domain.TicketReservation;
import nure.com.multievent.checkout.domain.TicketReservationItem;
import nure.com.multievent.checkout.domain.ReservationStatus;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.domain.Venue;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.catalog.infra.VenueRepository;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.iam.infra.RoleRepository;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TicketReservationRepositoryIntegrationTest {

    @Autowired
    private TicketReservationRepository ticketReservationRepository;

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
    void savesAndLoadsReservationGraph() {
        User customer = customer("buyer@example.com");
        Category category = categoryRepository.saveAndFlush(new Category("Concerts", "concerts"));
        Venue venue = venueRepository.saveAndFlush(new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine"));
        Event event = new Event(
                customer,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                EventStatus.PUBLISHED,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z"));
        event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
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
        ticketType.attachInventory(new nure.com.multievent.ticketing.domain.TicketInventory(ticketType, 100));
        ticketTypeRepository.saveAndFlush(ticketType);

        TicketReservation reservation = new TicketReservation(customer, event, Instant.parse("2026-08-15T00:15:00Z"));
        reservation.addItem(new TicketReservationItem(ticketType, 2));
        ticketReservationRepository.saveAndFlush(reservation);

        TicketReservation loaded = ticketReservationRepository.findByIdAndCustomer_Id(reservation.getId(), customer.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(ticketType.getInventory().getVersion()).isNotNull();
    }

    private User customer(String email) {
        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER)));
        return userRepository.saveAndFlush(User.register(email, "{noop}secret", role, new UserProfile(null, null, null, null, null)));
    }
}
