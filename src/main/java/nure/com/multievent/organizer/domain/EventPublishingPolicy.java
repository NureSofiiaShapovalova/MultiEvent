package nure.com.multievent.organizer.domain;

import java.time.Instant;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class EventPublishingPolicy {

    public void assertCanPublish(Event event) {
        if (event == null) {
            throw new BusinessRuleViolationException("Event must exist before publishing.");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Cancelled events cannot be published.");
        }
        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new BusinessRuleViolationException("Event is already published.");
        }
        if (event.getCategory() == null || event.getVenue() == null) {
            throw new BusinessRuleViolationException("Event must have a category and venue before publishing.");
        }
        if (event.getImages().isEmpty()) {
            throw new BusinessRuleViolationException("Event must have at least one image before publishing.");
        }
        if (event.getStartsAt() == null || event.getEndsAt() == null) {
            throw new BusinessRuleViolationException("Event must have start and end dates before publishing.");
        }
        if (!event.getEndsAt().isAfter(event.getStartsAt())) {
            throw new BusinessRuleViolationException("Event end date must be after the start date.");
        }
    }

    public void assertCanCancel(Event event) {
        if (event == null) {
            throw new BusinessRuleViolationException("Event must exist before cancelling.");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Event is already cancelled.");
        }
        if (event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleViolationException("Finished events cannot be cancelled.");
        }
    }

    public void markPublished(Event event, Instant publishedAt) {
        assertCanPublish(event);
        event.publish(publishedAt);
    }

    public void markCancelled(Event event, Instant cancelledAt) {
        assertCanCancel(event);
        event.cancel(cancelledAt);
    }
}
