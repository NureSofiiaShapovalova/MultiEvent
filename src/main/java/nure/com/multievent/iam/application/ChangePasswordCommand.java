package nure.com.multievent.iam.application;

public record ChangePasswordCommand(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
