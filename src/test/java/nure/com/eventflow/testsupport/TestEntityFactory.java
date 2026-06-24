package nure.com.eventflow.testsupport;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.shared.domain.BaseEntity;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketType;

public final class TestEntityFactory {

    private TestEntityFactory() {
    }

    public static User user(String email) {
        User user = User.register(email, "{noop}secret", new Role(RoleName.ROLE_USER), new UserProfile(null, null, null, null, null));
        setId(user, UUID.randomUUID());
        return user;
    }

    public static Category category(String name, String slug) {
        Category category = new Category(name, slug);
        setId(category, UUID.randomUUID());
        return category;
    }

    public static Venue venue(String name, String city) {
        Venue venue = new Venue(name, city, "Main street 1", "01001", "Ukraine");
        setId(venue, UUID.randomUUID());
        return venue;
    }

    public static Event event(User organizer, Category category, Venue venue, String slug, EventStatus status) {
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
        setId(event, UUID.randomUUID());
        event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        return event;
    }

    public static Event event(User organizer, Category category, Venue venue, EventStatus status) {
        return event(organizer, category, venue, "rock-night-" + UUID.randomUUID(), status);
    }

    public static TicketType ticketType(
            Event event,
            boolean active,
            int totalQuantity,
            BigDecimal price,
            int maxPerOrder,
            Instant salesStart,
            Instant salesEnd
    ) {
        TicketType ticketType = new TicketType(
                event,
                "Standard",
                "Entry ticket",
                price,
                "USD",
                maxPerOrder,
                salesStart,
                salesEnd,
                active);
        setId(ticketType, UUID.randomUUID());
        TicketInventory inventory = new TicketInventory(ticketType, totalQuantity);
        setId(inventory, UUID.randomUUID());
        ticketType.attachInventory(inventory);
        return ticketType;
    }

    public static void setId(Object target, UUID id) {
        setField(target, "id", id);
    }

    public static void setInstantFields(BaseEntity entity, Instant createdAt, Instant updatedAt) {
        setField(entity, "createdAt", createdAt);
        setField(entity, "updatedAt", updatedAt);
    }

    public static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to set field " + fieldName, e);
            }
        }
        throw new IllegalArgumentException("Field " + fieldName + " not found on " + target.getClass().getName());
    }
}
