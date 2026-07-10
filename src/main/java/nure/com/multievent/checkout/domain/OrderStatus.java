package nure.com.multievent.checkout.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    EXPIRED,
    CANCELLED,
    PAID,
    REFUND_PENDING,
    REFUNDED
}
