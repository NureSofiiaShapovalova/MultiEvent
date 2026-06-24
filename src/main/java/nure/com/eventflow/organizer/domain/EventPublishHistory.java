package nure.com.eventflow.organizer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "event_publish_history")
public class EventPublishHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventPublishAction action;

    @Column(length = 1000)
    private String notes;

    protected EventPublishHistory() {
    }

    public EventPublishHistory(Event event, User performedBy, EventPublishAction action, String notes) {
        this.event = event;
        this.performedBy = performedBy;
        this.action = action;
        this.notes = notes;
    }

    public Event getEvent() {
        return event;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public EventPublishAction getAction() {
        return action;
    }

    public String getNotes() {
        return notes;
    }
}
