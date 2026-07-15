package nure.com.multievent.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private ChangePasswordService changePasswordService;

    @BeforeEach
    void setUp() {
        changePasswordService = new ChangePasswordService(userRepository, passwordEncoder);
    }

    @Test
    void changesPasswordWhenCurrentPasswordIsValid() {
        User user = user("old-password");
        when(userRepository.findByEmail("sofi@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        changePasswordService.changeByEmail(
                " Sofi@example.COM ",
                new ChangePasswordCommand("old-password", "new-password", "new-password"));

        assertThat(passwordEncoder.matches("new-password", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-password", user.getPasswordHash())).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void rejectsInvalidCurrentPassword() {
        User user = user("old-password");
        when(userRepository.findByEmail("sofi@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> changePasswordService.changeByEmail(
                "sofi@example.com",
                new ChangePasswordCommand("wrong-password", "new-password", "new-password")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Current password is incorrect.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsPasswordConfirmationMismatch() {
        User user = user("old-password");
        when(userRepository.findByEmail("sofi@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> changePasswordService.changeByEmail(
                "sofi@example.com",
                new ChangePasswordCommand("old-password", "new-password", "different-password")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("New password and confirmation do not match.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void throwsWhenCurrentUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changePasswordService.changeByEmail(
                "missing@example.com",
                new ChangePasswordCommand("old-password", "new-password", "new-password")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Current user was not found.");
    }

    private User user(String password) {
        return User.register(
                "sofi@example.com",
                passwordEncoder.encode(password),
                new Role(RoleName.ROLE_USER),
                new UserProfile("Sofi", "Shapovalova", null, null, null));
    }
}
