package nure.com.eventflow.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class UserProfileRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findsProfileByUserId() {
        Role userRole = roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER));
        User user = User.register(
                "sofi@example.com",
                "{noop}secret",
                userRole,
                new UserProfile("Sofi", "Shapovalova", "+380000000000", "Kharkiv", "Bio"));
        User savedUser = userRepository.saveAndFlush(user);

        assertThat(userProfileRepository.findByUserId(savedUser.getId()))
                .isPresent()
                .get()
                .satisfies(profile -> {
                    assertThat(profile.getFirstName()).isEqualTo("Sofi");
                    assertThat(profile.getUser().getId()).isEqualTo(savedUser.getId());
                });
    }
}
