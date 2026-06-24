package nure.com.eventflow.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.admin.domain.AdminUserPolicy;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ToggleUserLockServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Clock clock;

    private ToggleUserLockService toggleUserLockService;

    @BeforeEach
    void setUp() {
        toggleUserLockService = new ToggleUserLockService(
                userRepository,
                auditLogRepository,
                new AdminUserPolicy(),
                new AdminViewMapper(),
                clock);
    }

    @Test
    void locksUserAndWritesAuditLog() {
        User admin = TestEntityFactory.user("admin@example.com");
        User target = TestEntityFactory.user("target@example.com");
        Instant now = Instant.parse("2026-06-23T16:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findWithRolesAndProfileById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        AdminUserRowView view = toggleUserLockService.lock("admin@example.com", target.getId());

        assertThat(target.isAccountNonLocked()).isFalse();
        assertThat(view.accountNonLocked()).isFalse();
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("LOCK_USER");
    }

    @Test
    void unlocksUserAndWritesAuditLog() {
        User admin = TestEntityFactory.user("admin@example.com");
        User target = TestEntityFactory.user("target@example.com");
        target.lockAccount();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findWithRolesAndProfileById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(Instant.parse("2026-06-23T16:05:00Z"));

        AdminUserRowView view = toggleUserLockService.unlock("admin@example.com", target.getId());

        assertThat(target.isAccountNonLocked()).isTrue();
        assertThat(view.accountNonLocked()).isTrue();
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("UNLOCK_USER");
    }

    @Test
    void preventsAdminFromLockingSelf() {
        User admin = TestEntityFactory.user("admin@example.com");
        UUID adminId = admin.getId();
        ReflectionTestUtils.setField(admin, "id", adminId);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findWithRolesAndProfileById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> toggleUserLockService.lock("admin@example.com", adminId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Admins cannot lock their own account.");
    }
}
