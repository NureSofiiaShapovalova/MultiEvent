package nure.com.multievent.payment.infra;

import nure.com.multievent.payment.domain.PaymentWebhookVerificationRequest;
import nure.com.multievent.payment.domain.PaymentWebhookVerificationResult;
import nure.com.multievent.payment.domain.PaymentWebhookVerifier;
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
