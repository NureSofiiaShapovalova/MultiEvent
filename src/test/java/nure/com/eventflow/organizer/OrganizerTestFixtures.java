package nure.com.eventflow.organizer;

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
import org.springframework.test.util.ReflectionTestUtils;

public final class OrganizerTestFixtures {

    private OrganizerTestFixtures() {
    }

    public static User user(String email, UUID id) {
        User user = User.register(email, "{noop}secret", new Role(RoleName.ROLE_USER), new UserProfile(null, null, null, null, null));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Category category() {
        Category category = new Category("Concerts", "concerts");
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }

    public static Venue venue() {
        Venue venue = new Venue("Palace", "Kyiv", "Main street 1", "01001", "Ukraine");
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
        if (status != EventStatus.CANCELLED) {
            event.replaceImages(List.of(new EventImage(event, "https://example.com/cover.jpg", true, 0)));
        }
        return event;
    }
}
