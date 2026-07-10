package nure.com.multievent.admin.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.multievent.admin.domain.AdminUserPolicy;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToggleUserLockService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminUserPolicy adminUserPolicy;
    private final AdminViewMapper adminViewMapper;
    private final Clock clock;

    public ToggleUserLockService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            AdminUserPolicy adminUserPolicy,
            AdminViewMapper adminViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminUserPolicy = adminUserPolicy;
        this.adminViewMapper = adminViewMapper;
        this.clock = clock;
    }

    @Transactional
    public AdminUserRowView lock(String adminEmail, UUID userId) {
        User admin = findUserByEmail(adminEmail);
        User target = findUserById(userId);
        adminUserPolicy.assertCanLock(admin, target);
        target.lockAccount();
        User savedTarget = userRepository.save(target);
        auditLogRepository.save(new AuditLog(
                admin,
                "User",
                savedTarget.getId(),
                "LOCK_USER",
                "Locked user account at " + clock.instant()));
        return adminViewMapper.toUserRowView(savedTarget);
    }

    @Transactional
    public AdminUserRowView unlock(String adminEmail, UUID userId) {
        User admin = findUserByEmail(adminEmail);
        User target = findUserById(userId);
        adminUserPolicy.assertCanUnlock(admin, target);
        target.unlockAccount();
        User savedTarget = userRepository.save(target);
        auditLogRepository.save(new AuditLog(
                admin,
                "User",
                savedTarget.getId(),
                "UNLOCK_USER",
                "Unlocked user account at " + clock.instant()));
        return adminViewMapper.toUserRowView(savedTarget);
    }

    private User findUserByEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private User findUserById(UUID userId) {
        return userRepository.findWithRolesAndProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found."));
    }
}
