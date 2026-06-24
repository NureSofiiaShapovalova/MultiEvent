package nure.com.eventflow.payment.domain;

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
