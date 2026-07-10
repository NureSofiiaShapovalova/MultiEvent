package nure.com.multievent.iam.application;

public record UpdateProfileCommand(
        String firstName,
        String lastName,
        String phoneNumber,
        String city,
        String bio
) {
}
