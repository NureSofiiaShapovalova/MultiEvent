package nure.com.eventflow.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.UserRegisteredEvent;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RegisterUserService registerUserService;

    @Test
    void registersUserWithDefaultRoleAndHashedPassword() {
        Role userRole = new Role(RoleName.ROLE_USER);
        RegisterUserCommand command = new RegisterUserCommand(
                "  Sofi@example.COM  ",
                "secret-password",
                " Sofi ",
                " Shapovalova ",
                " +380000000000 ",
                " Kharkiv ",
                " Event organizer ");

        when(userRepository.existsByEmail("sofi@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("secret-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            TestEntityFactory.setId(savedUser, UUID.randomUUID());
            return savedUser;
        });

        CurrentUserView view = registerUserService.register(command);

        assertThat(view.email()).isEqualTo("sofi@example.com");
        assertThat(view.firstName()).isEqualTo("Sofi");
        assertThat(view.lastName()).isEqualTo("Shapovalova");
        assertThat(view.phoneNumber()).isEqualTo("+380000000000");
        assertThat(view.city()).isEqualTo("Kharkiv");
        assertThat(view.bio()).isEqualTo("Event organizer");
        assertThat(view.roles()).containsExactly(RoleName.ROLE_USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("sofi@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getProfile().getFirstName()).isEqualTo("Sofi");
        assertThat(savedUser.getRoles()).containsExactly(userRole);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(savedUser.getId());
    }

    @Test
    void rejectsDuplicateEmailBeforeHashingPassword() {
        RegisterUserCommand command = new RegisterUserCommand(
                "sofi@example.com",
                "secret-password",
                null,
                null,
                null,
                null,
                null);

        when(userRepository.existsByEmail("sofi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registerUserService.register(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("A user with this email already exists.");

        verify(passwordEncoder, never()).encode(any());
        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsBlankEmail() {
        RegisterUserCommand command = new RegisterUserCommand(
                " ",
                "secret-password",
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> registerUserService.register(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Email is required.");

        verify(userRepository, never()).existsByEmail(any());
    }
}
