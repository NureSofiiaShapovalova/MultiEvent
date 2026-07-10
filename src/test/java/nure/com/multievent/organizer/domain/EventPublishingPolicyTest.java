package nure.com.multievent.organizer.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.organizer.OrganizerTestFixtures;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

class EventPublishingPolicyTest {

    private final EventPublishingPolicy eventPublishingPolicy = new EventPublishingPolicy();

    @Test
    void allowsPublishingReadyDraft() {
        var event = OrganizerTestFixtures.event(
                OrganizerTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                OrganizerTestFixtures.category(),
                OrganizerTestFixtures.venue(),
                EventStatus.DRAFT);

        eventPublishingPolicy.markPublished(event, Instant.parse("2026-08-15T10:00:00Z"));

        org.assertj.core.api.Assertions.assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void rejectsPublishingWithoutImages() {
        var organizer = OrganizerTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID());
        var category = OrganizerTestFixtures.category();
        var venue = OrganizerTestFixtures.venue();
        var event = new nure.com.multievent.catalog.domain.Event(
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

        assertThatThrownBy(() -> eventPublishingPolicy.markPublished(event, Instant.parse("2026-08-15T10:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Event must have at least one image before publishing.");
    }

    @Test
    void rejectsCancellingFinishedEvent() {
        var event = OrganizerTestFixtures.event(
                OrganizerTestFixtures.user("organizer@example.com", java.util.UUID.randomUUID()),
                OrganizerTestFixtures.category(),
                OrganizerTestFixtures.venue(),
                EventStatus.FINISHED);

        assertThatThrownBy(() -> eventPublishingPolicy.markCancelled(event, Instant.parse("2026-08-15T10:00:00Z")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Finished events cannot be cancelled.");
    }
}
