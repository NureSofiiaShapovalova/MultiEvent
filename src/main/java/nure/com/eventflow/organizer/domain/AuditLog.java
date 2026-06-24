package nure.com.eventflow.organizer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "text")
    private String details;

    protected AuditLog() {
    }

    public AuditLog(User actor, String targetType, UUID targetId, String action, String details) {
        this.actor = actor;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.details = details;
    }

    public User getActor() {
        return actor;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}
