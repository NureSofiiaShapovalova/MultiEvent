package nure.com.eventflow.notifications.application;

public record NotificationPreferencesCommand(
        boolean emailEnabled,
        boolean orderUpdatesEnabled,
        boolean marketingEnabled
) {
}
