package nure.com.eventflow.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
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
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void savesUserWithProfileAndRoleMapping() {
        Role userRole = roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER));
        User user = User.register(
                "sofi@example.com",
                "{noop}secret",
                userRole,
                new UserProfile("Sofi", "Shapovalova", "+380000000000", "Kharkiv", "Bio"));

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("sofi@example.com");
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_USER);
        assertThat(saved.getProfile()).isNotNull();
        assertThat(saved.getProfile().getUser().getId()).isEqualTo(saved.getId());

        assertThat(userRepository.existsByEmail("sofi@example.com")).isTrue();
        assertThat(userRepository.findByEmail("sofi@example.com"))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getEmail()).isEqualTo("sofi@example.com");
                    assertThat(found.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_USER);
                    assertThat(found.getProfile()).extracting(UserProfile::getCity).isEqualTo("Kharkiv");
                });
    }

    @Test
    void normalizesEmailDuringRegistration() {
        Role userRole = roleRepository.saveAndFlush(new Role(RoleName.ROLE_USER));

        User saved = userRepository.saveAndFlush(User.register(
                "  SOFI@EXAMPLE.COM  ",
                "{noop}secret",
                userRole,
                new UserProfile(null, null, null, null, null)));

        assertThat(saved.getEmail()).isEqualTo("sofi@example.com");
        assertThat(userRepository.findByEmail("sofi@example.com")).isPresent();
    }
}
