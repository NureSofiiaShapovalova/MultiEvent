package nure.com.eventflow.notifications.application;

import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.EventSubscriptionPreference;
import nure.com.eventflow.notifications.infra.SubscriptionPreferenceRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateNotificationPreferencesService {

    private final UserRepository userRepository;
    private final SubscriptionPreferenceRepository subscriptionPreferenceRepository;

    public UpdateNotificationPreferencesService(
            UserRepository userRepository,
            SubscriptionPreferenceRepository subscriptionPreferenceRepository
    ) {
        this.userRepository = userRepository;
        this.subscriptionPreferenceRepository = subscriptionPreferenceRepository;
    }

    @Transactional
    public NotificationPreferencesView getOrCreate(String email) {
        User user = findUser(email);
        EventSubscriptionPreference preferences = ensurePreferences(user);
        return toView(preferences);
    }

    @Transactional
    public NotificationPreferencesView update(String email, NotificationPreferencesCommand command) {
        User user = findUser(email);
        EventSubscriptionPreference preferences = ensurePreferences(user);
        preferences.update(
                command.emailEnabled(),
                command.orderUpdatesEnabled(),
                command.marketingEnabled());
        return toView(subscriptionPreferenceRepository.save(preferences));
    }

    private User findUser(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private EventSubscriptionPreference ensurePreferences(User user) {
        return subscriptionPreferenceRepository.findByUser_Id(user.getId())
                .orElseGet(() -> subscriptionPreferenceRepository.save(new EventSubscriptionPreference(user)));
    }

    private NotificationPreferencesView toView(EventSubscriptionPreference preferences) {
        return new NotificationPreferencesView(
                preferences.isEmailEnabled(),
                preferences.isOrderUpdatesEnabled(),
                preferences.isMarketingEnabled());
    }
}
