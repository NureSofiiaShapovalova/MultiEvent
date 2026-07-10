package nure.com.multievent.notifications.web;

public class NotificationPreferencesForm {

    private boolean emailEnabled;
    private boolean orderUpdatesEnabled;
    private boolean marketingEnabled;

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isOrderUpdatesEnabled() {
        return orderUpdatesEnabled;
    }

    public void setOrderUpdatesEnabled(boolean orderUpdatesEnabled) {
        this.orderUpdatesEnabled = orderUpdatesEnabled;
    }

    public boolean isMarketingEnabled() {
        return marketingEnabled;
    }

    public void setMarketingEnabled(boolean marketingEnabled) {
        this.marketingEnabled = marketingEnabled;
    }
}
