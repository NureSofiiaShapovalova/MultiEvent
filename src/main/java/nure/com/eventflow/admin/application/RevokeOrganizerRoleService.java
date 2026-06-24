package nure.com.eventflow.admin.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokeOrganizerRoleService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminViewMapper adminViewMapper;
    private final Clock clock;

    public RevokeOrganizerRoleService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            AdminViewMapper adminViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminViewMapper = adminViewMapper;
        this.clock = clock;
    }

    @Transactional
    public AdminUserRowView revoke(String adminEmail, UUID userId) {
        User admin = findAdmin(adminEmail);
        User user = userRepository.findWithRolesAndProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found."));

        if (user.hasRole(RoleName.ROLE_ORGANIZER)) {
            user.removeRole(RoleName.ROLE_ORGANIZER);
        }

        User savedUser = userRepository.save(user);
        auditLogRepository.save(new AuditLog(
                admin,
                "User",
                savedUser.getId(),
                "REVOKE_ORGANIZER_ROLE",
                "Revoked organizer role at " + clock.instant()));
        return adminViewMapper.toUserRowView(savedUser);
    }

    private User findAdmin(String adminEmail) {
        String normalizedEmail = EmailNormalizer.normalize(adminEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
