package nure.com.multievent.admin.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.RoleRepository;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantOrganizerRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminViewMapper adminViewMapper;
    private final Clock clock;

    public GrantOrganizerRoleService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditLogRepository auditLogRepository,
            AdminViewMapper adminViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminViewMapper = adminViewMapper;
        this.clock = clock;
    }

    @Transactional
    public AdminUserRowView grant(String adminEmail, UUID userId) {
        User admin = findAdmin(adminEmail);
        User user = userRepository.findWithRolesAndProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found."));
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER)
                .orElseThrow(() -> new NotFoundException("Organizer role was not found."));

        if (!user.hasRole(RoleName.ROLE_ORGANIZER)) {
            user.assignRole(organizerRole);
        }

        User savedUser = userRepository.save(user);
        auditLogRepository.save(new AuditLog(
                admin,
                "User",
                savedUser.getId(),
                "GRANT_ORGANIZER_ROLE",
                "Granted organizer role at " + clock.instant()));
        return adminViewMapper.toUserRowView(savedUser);
    }

    private User findAdmin(String adminEmail) {
        String normalizedEmail = EmailNormalizer.normalize(adminEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
