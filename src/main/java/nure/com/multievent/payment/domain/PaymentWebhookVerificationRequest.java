package nure.com.multievent.payment.domain;

import java.util.Map;

public record PaymentWebhookVerificationRequest(
        Map<String, String> headers,
        String body
) {
}
