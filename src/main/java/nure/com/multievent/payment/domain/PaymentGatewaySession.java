package nure.com.multievent.payment.domain;

public record PaymentGatewaySession(
        String providerName,
        String providerReference,
        String approvalUrl,
        String rawResponse
) {

    public PaymentGatewaySession(String providerName, String providerReference) {
        this(providerName, providerReference, null, null);
    }
}
