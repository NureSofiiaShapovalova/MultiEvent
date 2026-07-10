package nure.com.multievent.notifications.application;

public record NotificationPreferencesView(
        boolean emailEnabled,
        boolean orderUpdatesEnabled,
        boolean marketingEnabled
) {
}
