package nure.com.multievent.notifications.domain;

import java.util.UUID;

public record OrderPaidEvent(UUID orderId, UUID customerId) {
}
