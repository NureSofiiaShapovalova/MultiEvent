package nure.com.eventflow.notifications.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import nure.com.eventflow.iam.infra.RoleRepository;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.EventSubscriptionPreference;
import nure.com.eventflow.notifications.domain.NotificationChannel;
import nure.com.eventflow.notifications.domain.NotificationRecord;
import nure.com.eventflow.notifications.domain.NotificationStatus;
import nure.com.eventflow.notifications.domain.NotificationTemplateCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class NotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private SubscriptionPreferenceRepository subscriptionPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesNotificationLogPayloadAndCanReadItBack() {
        User user = user("buyer@example.com");
        NotificationRecord record = notificationLogRepository.saveAndFlush(new NotificationRecord(
                user,
                NotificationChannel.EMAIL,
                NotificationTemplateCode.ORDER_PAID,
                "{\"orderId\":\"order-123\",\"currency\":\"USD\"}"));

        entityManager.clear();

        List<NotificationRecord> records = notificationLogRepository.findByRecipient_IdOrderByCreatedAtDesc(user.getId());
        assertThat(records).hasSize(1);
        NotificationRecord loaded = records.get(0);
        assertThat(loaded.getId()).isEqualTo(record.getId());
        assertThat(loaded.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(loaded.getTemplateCode()).isEqualTo(NotificationTemplateCode.ORDER_PAID);
        assertThat(loaded.getPayloadJson()).isEqualTo("{\"orderId\":\"order-123\",\"currency\":\"USD\"}");
    }

    @Test
    void storesAndReadsSubscriptionPreferences() {
        User user = user("buyer@example.com");
        EventSubscriptionPreference preferences = subscriptionPreferenceRepository.saveAndFlush(
                new EventSubscriptionPreference(user, false, true, true));

        entityManager.clear();

        EventSubscriptionPreference loaded = subscriptionPreferenceRepository.findByUser_Id(user.getId())
                .orElseThrow();
        assertThat(loaded.getId()).isEqualTo(preferences.getId());
        assertThat(loaded.isEmailEnabled()).isFalse();
        assertThat(loaded.isOrderUpdatesEnabled()).isTrue();
        assertThat(loaded.isMarketingEnabled()).isTrue();
    }

    private User user(String email) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER)));
        return userRepository.saveAndFlush(User.register(
                email,
                "{noop}secret",
                userRole,
                new UserProfile(null, null, null, null, null)));
    }
}
