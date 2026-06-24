package nure.com.eventflow.notifications.application;

public record NotificationPreferencesView(
        boolean emailEnabled,
        boolean orderUpdatesEnabled,
        boolean marketingEnabled
) {
}
