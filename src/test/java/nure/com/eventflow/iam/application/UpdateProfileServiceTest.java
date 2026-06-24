package nure.com.eventflow.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateProfileService updateProfileService;

    @Test
    void updatesCurrentUsersProfile() {
        User user = User.register(
                "sofi@example.com",
                "hashed-password",
                new Role(RoleName.ROLE_USER),
                new UserProfile("Old", "Name", null, null, null));
        UpdateProfileCommand command = new UpdateProfileCommand(
                " Sofi ",
                " Shapovalova ",
                " +380000000000 ",
                " Kharkiv ",
                " ");

        when(userRepository.findByEmail("sofi@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrentUserView view = updateProfileService.updateByEmail("  Sofi@example.COM ", command);

        assertThat(view.email()).isEqualTo("sofi@example.com");
        assertThat(view.firstName()).isEqualTo("Sofi");
        assertThat(view.lastName()).isEqualTo("Shapovalova");
        assertThat(view.phoneNumber()).isEqualTo("+380000000000");
        assertThat(view.city()).isEqualTo("Kharkiv");
        assertThat(view.bio()).isNull();
        assertThat(user.getProfile().getFirstName()).isEqualTo("Sofi");

        verify(userRepository).findByEmail("sofi@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void throwsWhenCurrentUserDoesNotExist() {
        UpdateProfileCommand command = new UpdateProfileCommand(null, null, null, null, null);

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateProfileService.updateByEmail("missing@example.com", command))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Current user was not found.");
    }
}
