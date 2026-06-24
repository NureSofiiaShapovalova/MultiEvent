package nure.com.eventflow.notifications.domain;

import java.util.UUID;

public record EventPublishedEvent(UUID eventId, UUID organizerId) {
}
