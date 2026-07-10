package nure.com.multievent.iam.application;

import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProfileService {

    private final UserRepository userRepository;

    public UpdateProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public CurrentUserView updateByEmail(String email, UpdateProfileCommand command) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile(null, null, null, null, null);
            user.changeProfile(profile);
        }
        profile.update(
                command.firstName(),
                command.lastName(),
                command.phoneNumber(),
                command.city(),
                command.bio());

        return CurrentUserView.from(userRepository.save(user));
    }
}
