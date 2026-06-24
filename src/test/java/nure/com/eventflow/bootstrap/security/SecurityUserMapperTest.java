package nure.com.eventflow.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.domain.UserProfile;
import org.junit.jupiter.api.Test;

class SecurityUserMapperTest {

    private final SecurityUserMapper securityUserMapper = new SecurityUserMapper();

    @Test
    void mapsUserToSpringSecurityPrincipal() {
        Role userRole = new Role(RoleName.ROLE_USER);
        Role adminRole = new Role(RoleName.ROLE_ADMIN);
        User user = User.register(
                "sofi@example.com",
                "hashed-password",
                userRole,
                new UserProfile("Sofi", "Shapovalova", null, null, null));
        user.assignRole(adminRole);
        user.lockAccount();

        CustomUserDetails details = securityUserMapper.toUserDetails(user);

        assertThat(details.getUsername()).isEqualTo("sofi@example.com");
        assertThat(details.getEmail()).isEqualTo("sofi@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }
}
