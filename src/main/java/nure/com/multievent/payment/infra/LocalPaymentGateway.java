package nure.com.multievent.payment.infra;

import java.math.BigDecimal;
import java.util.UUID;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentGateway;
import nure.com.multievent.payment.domain.PaymentGatewayResult;
import nure.com.multievent.payment.domain.PaymentGatewaySession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "multievent.payment.provider", havingValue = "local", matchIfMissing = true)
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
