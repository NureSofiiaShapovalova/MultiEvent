package nure.com.eventflow.iam.application;

import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindCurrentUserService {

    private final UserRepository userRepository;

    public FindCurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserView findByEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .map(CurrentUserView::from)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
