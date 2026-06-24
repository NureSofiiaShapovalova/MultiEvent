package nure.com.eventflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import nure.com.eventflow.shared.domain.BaseEntity;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private PaymentTransactionType transactionType;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "provider_reference", nullable = false, length = 120)
    private String providerReference;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(
            Payment payment,
            PaymentTransactionType transactionType,
            String providerName,
            String providerReference,
            BigDecimal amount,
            String currency,
            String status,
            String rawPayload,
            Instant occurredAt
    ) {
        this.payment = Objects.requireNonNull(payment, "payment must not be null");
        this.transactionType = Objects.requireNonNull(transactionType, "transactionType must not be null");
        this.providerName = requireText(providerName, "providerName");
        this.providerReference = requireText(providerReference, "providerReference");
        this.amount = amount;
        this.currency = normalizeOptionalCurrency(currency);
        this.status = requireText(status, "status");
        this.rawPayload = rawPayload;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public Payment getPayment() {
        return payment;
    }

    public PaymentTransactionType getTransactionType() {
        return transactionType;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static String normalizeOptionalCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        String value = currency.trim().toUpperCase(Locale.ROOT);
        if (value.length() != 3) {
            throw new BusinessRuleViolationException("Currency must be a 3-letter code.");
        }
        return value;
    }
}
