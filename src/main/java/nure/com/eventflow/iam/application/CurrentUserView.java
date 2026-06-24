package nure.com.eventflow.iam.application;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;

public record CurrentUserView(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String city,
        String bio,
        Set<RoleName> roles
) {

    public static CurrentUserView from(User user) {
        UserProfile profile = user.getProfile();
        return new CurrentUserView(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getPhoneNumber() : null,
                profile != null ? profile.getCity() : null,
                profile != null ? profile.getBio() : null,
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toUnmodifiableSet()));
    }
}
