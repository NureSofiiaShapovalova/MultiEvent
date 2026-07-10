package nure.com.multievent.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.domain.UserProfile;
import nure.com.multievent.iam.infra.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUserMapper securityUserMapper;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadsUserByNormalizedEmail() {
        User user = User.register(
                "sofi@example.com",
                "hashed-password",
                new Role(RoleName.ROLE_USER),
                new UserProfile("Sofi", null, null, null, null));
        CustomUserDetails expectedDetails = new SecurityUserMapper().toUserDetails(user);

        when(userRepository.findByEmail("sofi@example.com")).thenReturn(Optional.of(user));
        when(securityUserMapper.toUserDetails(user)).thenReturn(expectedDetails);

        assertThat(customUserDetailsService.loadUserByUsername("  Sofi@example.COM  "))
                .isSameAs(expectedDetails);

        verify(userRepository).findByEmail("sofi@example.com");
        verify(securityUserMapper).toUserDetails(user);
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User was not found.");
    }

    @Test
    void throwsWhenUsernameIsBlank() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(" "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User was not found.");
    }
}
