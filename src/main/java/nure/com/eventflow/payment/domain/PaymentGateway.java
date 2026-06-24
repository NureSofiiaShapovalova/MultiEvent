package nure.com.eventflow.payment.domain;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentGatewaySession start(BigDecimal amount, String currency);

    default PaymentGatewaySession start(PaymentGatewayStartRequest request) {
        return start(request.amount(), request.currency());
    }

    PaymentGatewayResult confirm(Payment payment);
}
