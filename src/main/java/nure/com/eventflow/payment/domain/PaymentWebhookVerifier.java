package nure.com.eventflow.payment.domain;

public interface PaymentWebhookVerifier {

    PaymentWebhookVerificationResult verify(PaymentWebhookVerificationRequest request);
}
