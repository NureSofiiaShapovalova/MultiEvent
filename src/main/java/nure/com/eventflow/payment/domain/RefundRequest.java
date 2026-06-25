package nure.com.eventflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;

@Entity
@Table(name = "refund_requests")
public class RefundRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected RefundRequest() {
    }

    public RefundRequest(Order order, User requestedBy, String reason, Instant requestedAt) {
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        this.reason = requireText(reason, "reason");
        this.status = RefundStatus.REQUESTED;
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }

    public Order getOrder() {
        return order;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void approve(Instant resolvedAt) {
        ensureRequested();
        this.status = RefundStatus.APPROVED;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    public void reject(Instant resolvedAt) {
        ensureRequested();
        this.status = RefundStatus.REJECTED;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    public void complete(Instant resolvedAt) {
        if (status != RefundStatus.APPROVED) {
            throw new BusinessRuleViolationException("Refund request is not approved.");
        }
        this.status = RefundStatus.COMPLETED;
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    private void ensureRequested() {
        if (status != RefundStatus.REQUESTED) {
            throw new BusinessRuleViolationException("Refund request is no longer waiting for review.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
