package nure.com.multievent.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import nure.com.multievent.shared.domain.BaseEntity;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;

@Entity
@Table(
        name = "provider_webhook_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_provider_webhook_log_provider_event",
                columnNames = {"provider_name", "event_id"}
        )
)
public class ProviderWebhookLog extends BaseEntity {

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private WebhookVerificationStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private WebhookProcessingStatus processingStatus;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected ProviderWebhookLog() {
    }

    public ProviderWebhookLog(
            String providerName,
            String eventId,
            String eventType,
            String resourceId,
            String rawPayload,
            Instant receivedAt
    ) {
        this.providerName = requireText(providerName, "providerName");
        this.eventId = requireText(eventId, "eventId");
        this.eventType = requireText(eventType, "eventType");
        this.resourceId = normalizeOptionalText(resourceId);
        this.rawPayload = requireText(rawPayload, "rawPayload");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        this.verificationStatus = WebhookVerificationStatus.PENDING;
        this.processingStatus = WebhookProcessingStatus.PENDING;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public WebhookVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public WebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public boolean isFinished() {
        return processingStatus != WebhookProcessingStatus.PENDING;
    }

    public void markVerified() {
        this.verificationStatus = WebhookVerificationStatus.VERIFIED;
        this.failureReason = null;
    }

    public void markVerificationFailed(String failureReason, Instant processedAt) {
        this.verificationStatus = WebhookVerificationStatus.FAILED;
        this.processingStatus = WebhookProcessingStatus.FAILED;
        this.failureReason = requireText(failureReason, "failureReason");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
    }

    public void markProcessed(Instant processedAt) {
        this.processingStatus = WebhookProcessingStatus.PROCESSED;
        this.failureReason = null;
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
    }

    public void markIgnored(String reason, Instant processedAt) {
        this.processingStatus = WebhookProcessingStatus.IGNORED;
        this.failureReason = requireText(reason, "reason");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
    }

    public void markFailed(String failureReason, Instant processedAt) {
        this.processingStatus = WebhookProcessingStatus.FAILED;
        this.failureReason = requireText(failureReason, "failureReason");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
