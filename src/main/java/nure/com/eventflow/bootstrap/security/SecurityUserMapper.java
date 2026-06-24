package nure.com.eventflow.bootstrap.security;

import java.util.Comparator;
import java.util.List;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SecurityUserMapper {

    public CustomUserDetails toUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .sorted(Comparator.comparing(Enum::name))
                .map(roleName -> new SimpleGrantedAuthority(roleName.name()))
                .toList();

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                authorities);
    }
}
