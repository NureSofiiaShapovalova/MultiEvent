package nure.com.multievent.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import nure.com.multievent.shared.domain.BaseEntity;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;

@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_attempts_payment_number",
                columnNames = {"payment_id", "attempt_number"}
        )
)
public class PaymentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 20)
    private PaymentAttemptType attemptType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "provider_reference", nullable = false, length = 100)
    private String providerReference;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(
            PaymentAttemptType attemptType,
            PaymentStatus status,
            String providerName,
            String providerReference,
            BigDecimal requestedAmount,
            String currency,
            String failureReason
    ) {
        this.attemptType = Objects.requireNonNull(attemptType, "attemptType must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.providerName = requireText(providerName, "providerName");
        this.providerReference = requireText(providerReference, "providerReference");
        this.requestedAmount = requireAmount(requestedAmount);
        this.currency = normalizeCurrency(currency);
        this.failureReason = failureReason;
    }

    public Payment getPayment() {
        return payment;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public PaymentAttemptType getAttemptType() {
        return attemptType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFailureReason() {
        return failureReason;
    }

    void assignAttemptNumber(int attemptNumber) {
        if (attemptNumber <= 0) {
            throw new BusinessRuleViolationException("Attempt number must be positive.");
        }
        this.attemptNumber = attemptNumber;
    }

    void attachTo(Payment payment) {
        this.payment = Objects.requireNonNull(payment, "payment must not be null");
    }

    private static BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleViolationException("Requested amount must be positive.");
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
}
