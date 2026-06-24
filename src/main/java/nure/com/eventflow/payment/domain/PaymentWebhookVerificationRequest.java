package nure.com.eventflow.payment.domain;

import java.util.Map;

public record PaymentWebhookVerificationRequest(
        Map<String, String> headers,
        String body
) {
}
