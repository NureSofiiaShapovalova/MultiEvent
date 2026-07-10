package nure.com.multievent.checkout;

import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.ticketing.TicketingTestFixtures;
import nure.com.multievent.ticketing.domain.TicketType;
import org.springframework.test.util.ReflectionTestUtils;

public final class CheckoutTestFixtures {

    private CheckoutTestFixtures() {
    }

    public static User customer(String email, UUID id) {
        return TicketingTestFixtures.user(email, id);
    }

    public static TicketType ticketType(Event event, boolean active) {
        return TicketingTestFixtures.ticketType(
                event,
                "Standard",
                active,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                java.math.BigDecimal.valueOf(25),
                "USD",
                4,
                100,
                0,
                0);
    }

    public static void assignId(Object target, UUID id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
