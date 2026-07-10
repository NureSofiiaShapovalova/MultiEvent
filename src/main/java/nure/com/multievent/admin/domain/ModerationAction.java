package nure.com.multievent.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.shared.domain.BaseEntity;

@Entity
@Table(name = "moderation_actions")
public class ModerationAction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationActionType action;

    @Column(columnDefinition = "text")
    private String reason;

    protected ModerationAction() {
    }

    public ModerationAction(User actor, Event event, ModerationActionType action, String reason) {
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
        this.event = Objects.requireNonNull(event, "event must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    public User getActor() {
        return actor;
    }

    public Event getEvent() {
        return event;
    }

    public ModerationActionType getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }
}
