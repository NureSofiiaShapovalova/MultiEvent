package nure.com.eventflow.notifications.domain;

import java.util.UUID;

public record OrderPaidEvent(UUID orderId, UUID customerId) {
}
