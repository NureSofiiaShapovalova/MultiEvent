package nure.com.eventflow.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "event_subscription_preferences")
public class EventSubscriptionPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "order_updates_enabled", nullable = false)
    private boolean orderUpdatesEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled = false;

    protected EventSubscriptionPreference() {
    }

    public EventSubscriptionPreference(User user) {
        this(user, true, true, false);
    }

    public EventSubscriptionPreference(
            User user,
            boolean emailEnabled,
            boolean orderUpdatesEnabled,
            boolean marketingEnabled
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.emailEnabled = emailEnabled;
        this.orderUpdatesEnabled = orderUpdatesEnabled;
        this.marketingEnabled = marketingEnabled;
    }

    public User getUser() {
        return user;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isOrderUpdatesEnabled() {
        return orderUpdatesEnabled;
    }

    public boolean isMarketingEnabled() {
        return marketingEnabled;
    }

    public void update(boolean emailEnabled, boolean orderUpdatesEnabled, boolean marketingEnabled) {
        this.emailEnabled = emailEnabled;
        this.orderUpdatesEnabled = orderUpdatesEnabled;
        this.marketingEnabled = marketingEnabled;
    }
}
