package nure.com.multievent.iam.application;

public record RegisterUserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber,
        String city,
        String bio
) {
}
