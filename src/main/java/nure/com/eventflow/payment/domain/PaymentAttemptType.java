package nure.com.eventflow.payment.domain;

public enum PaymentAttemptType {
    START,
    CONFIRM,
    RETRY,
    WEBHOOK
}
