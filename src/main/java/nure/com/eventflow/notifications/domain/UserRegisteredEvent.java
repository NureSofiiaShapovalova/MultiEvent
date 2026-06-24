package nure.com.eventflow.notifications.domain;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId) {
}
