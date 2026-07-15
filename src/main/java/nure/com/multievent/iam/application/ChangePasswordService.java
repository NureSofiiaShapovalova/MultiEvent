package nure.com.multievent.iam.application;

import java.util.Objects;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changeByEmail(String email, ChangePasswordCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String normalizedEmail = EmailNormalizer.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));

        String currentPassword = requirePassword(command.currentPassword(), "Current password is required.");
        String newPassword = requirePassword(command.newPassword(), "New password is required.");
        String confirmPassword = requirePassword(command.confirmPassword(), "Password confirmation is required.");

        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new BusinessRuleViolationException("New password must be between 8 and 100 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessRuleViolationException("New password and confirmation do not match.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Current password is incorrect.");
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private static String requirePassword(String password, String message) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleViolationException(message);
        }
        return password;
    }
}
