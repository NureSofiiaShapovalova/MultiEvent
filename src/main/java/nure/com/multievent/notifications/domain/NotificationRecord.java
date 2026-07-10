package nure.com.multievent.notifications.domain;

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
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.shared.domain.BaseEntity;

@Entity
@Table(name = "notification_log")
public class NotificationRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false, length = 50)
    private NotificationTemplateCode templateCode;

    @Column(name = "payload_jsonb", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected NotificationRecord() {
    }

    public NotificationRecord(
            User recipient,
            NotificationChannel channel,
            NotificationTemplateCode templateCode,
            String payloadJson
    ) {
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.templateCode = Objects.requireNonNull(templateCode, "templateCode must not be null");
        this.payloadJson = requireText(payloadJson, "payloadJson");
        this.status = NotificationStatus.PENDING;
    }

    public User getRecipient() {
        return recipient;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationTemplateCode getTemplateCode() {
        return templateCode;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }

    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }

    public void markSent(Instant sentAt) {
        this.status = NotificationStatus.SENT;
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = requireText(failureReason, "failureReason");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
