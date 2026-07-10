package nure.com.multievent.bootstrap.security;

import nure.com.multievent.iam.infra.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SecurityUserMapper securityUserMapper;

    public CustomUserDetailsService(UserRepository userRepository, SecurityUserMapper securityUserMapper) {
        this.userRepository = userRepository;
        this.securityUserMapper = securityUserMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String normalizedEmail = normalizeEmail(username);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new UsernameNotFoundException("User was not found.");
        }

        return userRepository.findByEmail(normalizedEmail)
                .map(securityUserMapper::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User was not found."));
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
