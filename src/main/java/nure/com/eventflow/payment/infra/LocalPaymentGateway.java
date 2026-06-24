package nure.com.eventflow.payment.infra;

import java.math.BigDecimal;
import java.util.UUID;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentGateway;
import nure.com.eventflow.payment.domain.PaymentGatewayResult;
import nure.com.eventflow.payment.domain.PaymentGatewaySession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eventflow.payment.provider", havingValue = "local", matchIfMissing = true)
public class LocalPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewaySession start(BigDecimal amount, String currency) {
        return new PaymentGatewaySession("LOCAL_MANUAL", "local-" + UUID.randomUUID());
    }

    @Override
    public PaymentGatewayResult confirm(Payment payment) {
        return new PaymentGatewayResult(true, null);
    }
}
