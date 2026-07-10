package nure.com.multievent.notifications.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import nure.com.multievent.notifications.domain.NotificationTemplateCode;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateRenderer {

    public RenderedNotification render(NotificationTemplateCode templateCode, Map<String, ?> payload) {
        Objects.requireNonNull(templateCode, "templateCode must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        return switch (templateCode) {
            case WELCOME -> welcome(payload);
            case EVENT_PUBLISHED -> eventPublished(payload);
            case ORDER_PAID -> orderPaid(payload);
            case EVENT_CANCELLED -> eventCancelled(payload);
        };
    }

    private RenderedNotification welcome(Map<String, ?> payload) {
        String email = text(payload, "email");
        return new RenderedNotification(
                "Welcome to MultiEvent",
                """
                        Hi %s,

                        Thanks for joining MultiEvent. Your account for %s is ready.
                        """.formatted(email, email));
    }

    private RenderedNotification eventPublished(Map<String, ?> payload) {
        String eventTitle = text(payload, "eventTitle");
        String eventSlug = text(payload, "eventSlug");
        Instant publishedAt = instant(payload, "publishedAt");
        return new RenderedNotification(
                "Your event is now published: " + eventTitle,
                """
                        Your event "%s" is now visible to guests.

                        Slug: %s
                        Published at: %s
                        """.formatted(eventTitle, eventSlug, DateTimeFormatter.ISO_INSTANT.format(publishedAt)));
    }

    private RenderedNotification orderPaid(Map<String, ?> payload) {
        String eventTitle = text(payload, "eventTitle");
        String orderId = text(payload, "orderId");
        BigDecimal totalAmount = decimal(payload, "totalAmount");
        String currency = text(payload, "currency");
        return new RenderedNotification(
                "Payment received for " + eventTitle,
                """
                        Your order %s has been paid.

                        Event: %s
                        Total: %s %s
                        """.formatted(orderId, eventTitle, totalAmount.toPlainString(), currency));
    }

    private RenderedNotification eventCancelled(Map<String, ?> payload) {
        String eventTitle = text(payload, "eventTitle");
        String eventSlug = text(payload, "eventSlug");
        int orderCount = number(payload, "orderCount");
        return new RenderedNotification(
                "Event cancelled: " + eventTitle,
                """
                        The event "%s" was cancelled.

                        Slug: %s
                        Affected orders: %d
                        """.formatted(eventTitle, eventSlug, orderCount));
    }

    private String text(Map<String, ?> payload, String key) {
        Object value = payload.get(key);
        return Objects.toString(value, "");
    }

    private Instant instant(Map<String, ?> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalArgumentException("Payload value '" + key + "' must be an Instant.");
    }

    private BigDecimal decimal(Map<String, ?> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new IllegalArgumentException("Payload value '" + key + "' must be numeric.");
    }

    private int number(Map<String, ?> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Payload value '" + key + "' must be numeric.");
    }
}
