package nure.com.eventflow.notifications.domain;

import java.util.UUID;

public record EventCancelledEvent(UUID eventId, UUID organizerId) {
}
