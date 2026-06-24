package nure.com.eventflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "provider_reference", nullable = false, length = 100)
    private String providerReference;

    @Column(name = "provider_approval_url", columnDefinition = "text")
    private String providerApprovalUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private final List<PaymentAttempt> attempts = new ArrayList<>();

    protected Payment() {
    }

    public Payment(
            User customer,
            Order order,
            String providerName,
            String providerReference,
            BigDecimal amount,
            String currency,
            Instant startedAt
    ) {
        this(customer, order, providerName, providerReference, null, amount, currency, startedAt);
    }

    public Payment(
            User customer,
            Order order,
            String providerName,
            String providerReference,
            String providerApprovalUrl,
            BigDecimal amount,
            String currency,
            Instant startedAt
    ) {
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.providerName = requireText(providerName, "providerName");
        this.providerReference = requireText(providerReference, "providerReference");
        this.providerApprovalUrl = normalizeOptionalText(providerApprovalUrl);
        this.amount = requireAmount(amount);
        this.currency = normalizeCurrency(currency);
        this.status = PaymentStatus.PENDING;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public Order getOrder() {
        return order;
    }

    public User getCustomer() {
        return customer;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getProviderApprovalUrl() {
        return providerApprovalUrl;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<PaymentAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public void addAttempt(PaymentAttempt attempt) {
        PaymentAttempt nonNullAttempt = Objects.requireNonNull(attempt, "attempt must not be null");
        nonNullAttempt.assignAttemptNumber(attempts.size() + 1);
        attempts.add(nonNullAttempt);
        nonNullAttempt.attachTo(this);
    }

    public void markPaid(Instant completedAt) {
        ensurePending();
        this.status = PaymentStatus.PAID;
        this.failureReason = null;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    public void markFailed(String failureReason, Instant completedAt) {
        ensurePending();
        this.status = PaymentStatus.FAILED;
        this.failureReason = requireText(failureReason, "failureReason");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    public void retry(String providerName, String providerReference, Instant startedAt) {
        retry(providerName, providerReference, null, startedAt);
    }

    public void retry(String providerName, String providerReference, String providerApprovalUrl, Instant startedAt) {
        if (!isFailed()) {
            throw new BusinessRuleViolationException("Payment can only be retried after a failure.");
        }
        this.providerName = requireText(providerName, "providerName");
        this.providerReference = requireText(providerReference, "providerReference");
        this.providerApprovalUrl = normalizeOptionalText(providerApprovalUrl);
        this.status = PaymentStatus.PENDING;
        this.failureReason = null;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.completedAt = null;
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new BusinessRuleViolationException("Payment is no longer pending.");
        }
    }

    private static BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleViolationException("Payment amount must be positive.");
        }
        return amount;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static String normalizeCurrency(String currency) {
        String value = requireText(currency, "currency");
        if (value.length() != 3) {
            throw new BusinessRuleViolationException("Currency must be a 3-letter code.");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
