package nure.com.multievent.notifications.application;

public record NotificationPreferencesCommand(
        boolean emailEnabled,
        boolean orderUpdatesEnabled,
        boolean marketingEnabled
) {
}
