package nure.com.eventflow.payment.domain;

public record PaymentWebhookVerificationResult(
        boolean verified,
        String failureReason
) {
}
