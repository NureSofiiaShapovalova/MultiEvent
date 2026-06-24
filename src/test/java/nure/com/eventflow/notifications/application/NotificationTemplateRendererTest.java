package nure.com.eventflow.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import nure.com.eventflow.notifications.domain.NotificationTemplateCode;
import org.junit.jupiter.api.Test;

class NotificationTemplateRendererTest {

    private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();

    @Test
    void rendersWelcomeMail() {
        RenderedNotification notification = renderer.render(
                NotificationTemplateCode.WELCOME,
                Map.of("email", "buyer@example.com"));

        assertThat(notification.subject()).isEqualTo("Welcome to EventFlow");
        assertThat(notification.body()).contains("buyer@example.com");
    }

    @Test
    void rendersEventPublishedMail() {
        Instant publishedAt = Instant.parse("2026-06-22T12:00:00Z");
        RenderedNotification notification = renderer.render(
                NotificationTemplateCode.EVENT_PUBLISHED,
                Map.of(
                        "eventTitle", "Rock Night",
                        "eventSlug", "rock-night",
                        "publishedAt", publishedAt));

        assertThat(notification.subject()).contains("Rock Night");
        assertThat(notification.body()).contains("rock-night");
        assertThat(notification.body()).contains("2026-06-22T12:00:00Z");
    }

    @Test
    void rendersOrderPaidMail() {
        RenderedNotification notification = renderer.render(
                NotificationTemplateCode.ORDER_PAID,
                Map.of(
                        "eventTitle", "Rock Night",
                        "orderId", "order-123",
                        "totalAmount", BigDecimal.valueOf(50),
                        "currency", "USD"));

        assertThat(notification.subject()).contains("Rock Night");
        assertThat(notification.body()).contains("order-123");
        assertThat(notification.body()).contains("50");
        assertThat(notification.body()).contains("USD");
    }

    @Test
    void rendersEventCancelledMail() {
        RenderedNotification notification = renderer.render(
                NotificationTemplateCode.EVENT_CANCELLED,
                Map.of(
                        "eventTitle", "Rock Night",
                        "eventSlug", "rock-night",
                        "orderCount", 3));

        assertThat(notification.subject()).contains("Rock Night");
        assertThat(notification.body()).contains("rock-night");
        assertThat(notification.body()).contains("3");
    }
}
