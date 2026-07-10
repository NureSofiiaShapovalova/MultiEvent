package nure.com.multievent.ticketing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.domain.Venue;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.domain.TicketType;
import org.springframework.test.util.ReflectionTestUtils;

public final class TicketingTestFixtures {

    private TicketingTestFixtures() {
    }

    public static User user(String email, UUID id) {
        User user = User.register(email, "{noop}secret", new Role(RoleName.ROLE_USER), new UserProfile(null, null, null, null, null));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Category category(String name, String slug) {
        Category category = new Category(name, slug);
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }

    public static Venue venue(String name, String city) {
        Venue venue = new Venue(name, city, "Main street 1", "01001", "Ukraine");
        ReflectionTestUtils.setField(venue, "id", UUID.randomUUID());
        return venue;
    }

    public static Event event(User organizer, Category category, Venue venue, EventStatus status) {
        Event event = new Event(
                organizer,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                status,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                status == EventStatus.PUBLISHED ? Instant.parse("2026-08-01T10:00:00Z") : null);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        event.replaceImages(java.util.List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        return event;
    }

    public static TicketType ticketType(
            Event event,
            String name,
            boolean active,
            Instant salesStart,
            Instant salesEnd,
            BigDecimal price,
            String currency,
            int maxPerOrder,
            int totalQuantity,
            int reservedQuantity,
            int soldQuantity
    ) {
        TicketType ticketType = new TicketType(
                event,
                name,
                "Entry ticket",
                price,
                currency,
                maxPerOrder,
                salesStart,
                salesEnd,
                active);
        ReflectionTestUtils.setField(ticketType, "id", UUID.randomUUID());
        TicketInventory inventory = new TicketInventory(ticketType, totalQuantity);
        ReflectionTestUtils.setField(inventory, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(inventory, "reservedQuantity", reservedQuantity);
        ReflectionTestUtils.setField(inventory, "soldQuantity", soldQuantity);
        ticketType.attachInventory(inventory);
        return ticketType;
    }
}
