package nure.com.multievent.payment.domain;

public interface PaymentWebhookVerifier {

    PaymentWebhookVerificationResult verify(PaymentWebhookVerificationRequest request);
}
