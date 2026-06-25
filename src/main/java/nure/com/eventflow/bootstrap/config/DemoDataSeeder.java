package nure.com.eventflow.bootstrap.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.catalog.infra.VenueRepository;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "eventflow.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_EVENT_SLUG = "eventflow-demo-concert";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminEmail;
    private final String adminPassword;
    private final String organizerEmail;
    private final String organizerPassword;
    private final String userEmail;
    private final String userPassword;

    public DemoDataSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CategoryRepository categoryRepository,
            VenueRepository venueRepository,
            EventRepository eventRepository,
            TicketTypeRepository ticketTypeRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${eventflow.demo.admin-email}") String adminEmail,
            @Value("${eventflow.demo.admin-password}") String adminPassword,
            @Value("${eventflow.demo.organizer-email}") String organizerEmail,
            @Value("${eventflow.demo.organizer-password}") String organizerPassword,
            @Value("${eventflow.demo.user-email}") String userEmail,
            @Value("${eventflow.demo.user-password}") String userPassword
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.organizerEmail = organizerEmail;
        this.organizerPassword = organizerPassword;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role userRole = role(RoleName.ROLE_USER);
        Role organizerRole = role(RoleName.ROLE_ORGANIZER);
        Role adminRole = role(RoleName.ROLE_ADMIN);

        ensureUser(userEmail, userPassword, "Demo", "User", List.of(userRole));
        ensureUser(adminEmail, adminPassword, "Demo", "Admin", List.of(userRole, adminRole));
        User organizer = ensureUser(
                organizerEmail,
                organizerPassword,
                "Demo",
                "Organizer",
                List.of(userRole, organizerRole));

        if (!eventRepository.existsBySlug(DEMO_EVENT_SLUG)) {
            createDemoEvent(organizer);
        }
    }

    private User ensureUser(String email, String password, String firstName, String lastName, List<Role> roles) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseGet(() -> User.register(
                        email,
                        passwordEncoder.encode(password),
                        roles.get(0),
                        new UserProfile(firstName, lastName, null, "Kyiv", "Demo account for EventFlow local testing.")));
        roles.forEach(role -> {
            if (!user.hasRole(role.getName())) {
                user.assignRole(role);
            }
        });
        return userRepository.save(user);
    }

    private void createDemoEvent(User organizer) {
        Instant now = clock.instant();
        Instant startsAt = now.plus(21, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(3, ChronoUnit.HOURS);
        Category category = categoryRepository.findBySlugIgnoreCase("concerts")
                .orElseGet(() -> categoryRepository.save(new Category("Concerts", "concerts")));
        Venue venue = venueRepository.save(new Venue("EventFlow Demo Hall", "Kyiv", "Demo street 1", "01001", "Ukraine"));
        Event event = new Event(
                organizer,
                category,
                venue,
                "EventFlow Demo Concert",
                DEMO_EVENT_SLUG,
                "A demo published event with tickets ready for local checkout testing.",
                "Use this event to smoke-test public catalog, ticket selection, checkout, payment, and order flows.",
                EventStatus.PUBLISHED,
                startsAt,
                endsAt,
                now);
        event.replaceImages(List.of(new EventImage(event, "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1400&q=80", true, 0)));
        Event savedEvent = eventRepository.save(event);

        TicketType standard = new TicketType(
                savedEvent,
                "Standard",
                "General admission demo ticket",
                BigDecimal.valueOf(25),
                "EUR",
                4,
                now.minus(1, ChronoUnit.DAYS),
                startsAt.minus(1, ChronoUnit.HOURS),
                true);
        standard.attachInventory(new TicketInventory(standard, 100));
        ticketTypeRepository.save(standard);
    }

    private Role role(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
    }
}
