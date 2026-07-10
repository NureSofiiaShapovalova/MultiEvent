package nure.com.multievent.notifications.domain;

public enum NotificationTemplateCode {
    WELCOME(true, false, false),
    EVENT_PUBLISHED(true, false, false),
    ORDER_PAID(true, true, false),
    EVENT_CANCELLED(true, true, false);

    private final boolean requiresEmailEnabled;
    private final boolean requiresOrderUpdatesEnabled;
    private final boolean requiresMarketingEnabled;

    NotificationTemplateCode(boolean requiresEmailEnabled, boolean requiresOrderUpdatesEnabled, boolean requiresMarketingEnabled) {
        this.requiresEmailEnabled = requiresEmailEnabled;
        this.requiresOrderUpdatesEnabled = requiresOrderUpdatesEnabled;
        this.requiresMarketingEnabled = requiresMarketingEnabled;
    }

    public boolean requiresEmailEnabled() {
        return requiresEmailEnabled;
    }

    public boolean requiresOrderUpdatesEnabled() {
        return requiresOrderUpdatesEnabled;
    }

    public boolean requiresMarketingEnabled() {
        return requiresMarketingEnabled;
    }
}
