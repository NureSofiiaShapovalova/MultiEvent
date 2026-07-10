package nure.com.multievent.payment.domain;

public record PaymentWebhookVerificationResult(
        boolean verified,
        String failureReason
) {
}
