package nure.com.multievent.payment.domain;

public record PaymentGatewayResult(
        PaymentStatus status,
        String failureReason,
        String providerTransactionReference,
        String rawResponse
) {

    public PaymentGatewayResult(boolean successful, String failureReason) {
        this(successful ? PaymentStatus.PAID : PaymentStatus.FAILED, failureReason, null, null);
    }

    public boolean successful() {
        return status == PaymentStatus.PAID;
    }
}
