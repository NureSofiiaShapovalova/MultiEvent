package nure.com.eventflow.payment.infra;

import nure.com.eventflow.payment.domain.PaymentWebhookVerificationRequest;
import nure.com.eventflow.payment.domain.PaymentWebhookVerificationResult;
import nure.com.eventflow.payment.domain.PaymentWebhookVerifier;
import org.springframework.stereotype.Component;

@Component
public class PaypalWebhookVerifier implements PaymentWebhookVerifier {

    private final PaypalClient paypalClient;

    public PaypalWebhookVerifier(PaypalClient paypalClient) {
        this.paypalClient = paypalClient;
    }

    @Override
    public PaymentWebhookVerificationResult verify(PaymentWebhookVerificationRequest request) {
        return paypalClient.verifyWebhookSignature(request.headers(), request.body());
    }
}
