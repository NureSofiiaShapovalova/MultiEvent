package nure.com.multievent.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class RoleRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findsRoleByName() {
        roleRepository.saveAndFlush(new Role(RoleName.ROLE_ADMIN));

        assertThat(roleRepository.findByName(RoleName.ROLE_ADMIN))
                .isPresent()
                .get()
                .extracting(Role::getName)
                .isEqualTo(RoleName.ROLE_ADMIN);
    }
}
