package nure.com.multievent.notifications.domain;

import java.util.UUID;

public record EventPublishedEvent(UUID eventId, UUID organizerId) {
}
