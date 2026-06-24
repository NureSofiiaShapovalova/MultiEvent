package nure.com.eventflow.payment.infra;

import java.math.BigDecimal;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentGateway;
import nure.com.eventflow.payment.domain.PaymentGatewayResult;
import nure.com.eventflow.payment.domain.PaymentGatewaySession;
import nure.com.eventflow.payment.domain.PaymentGatewayStartRequest;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eventflow.payment.provider", havingValue = "paypal")
public class PaypalPaymentGateway implements PaymentGateway {

    private final PaypalClient paypalClient;

    public PaypalPaymentGateway(PaypalClient paypalClient) {
        this.paypalClient = paypalClient;
    }

    @Override
    public PaymentGatewaySession start(BigDecimal amount, String currency) {
        throw new BusinessRuleViolationException("PayPal payment start requires an order id.");
    }

    @Override
    public PaymentGatewaySession start(PaymentGatewayStartRequest request) {
        return paypalClient.createOrder(request);
    }

    @Override
    public PaymentGatewayResult confirm(Payment payment) {
        return paypalClient.captureOrder(payment);
    }
}
