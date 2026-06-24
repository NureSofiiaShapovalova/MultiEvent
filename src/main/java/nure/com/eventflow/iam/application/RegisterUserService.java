package nure.com.eventflow.iam.application;

import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.UserRegisteredEvent;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RegisterUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public CurrentUserView register(RegisterUserCommand command) {
        String normalizedEmail = requireEmail(command.email());
        requirePassword(command.password());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleViolationException("A user with this email already exists.");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new NotFoundException("Default user role was not found."));
        UserProfile profile = new UserProfile(
                command.firstName(),
                command.lastName(),
                command.phoneNumber(),
                command.city(),
                command.bio());
        User user = User.register(
                normalizedEmail,
                passwordEncoder.encode(command.password()),
                userRole,
                profile);
        User savedUser = userRepository.save(user);
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getId()));
        return CurrentUserView.from(savedUser);
    }

    private static String requireEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new BusinessRuleViolationException("Email is required.");
        }
        return normalizedEmail;
    }

    private static void requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleViolationException("Password is required.");
        }
    }
}
