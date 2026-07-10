package nure.com.multievent.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.RoleRepository;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminRoleServicesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Clock clock;

    private AdminViewMapper adminViewMapper;
    private GrantOrganizerRoleService grantOrganizerRoleService;
    private RevokeOrganizerRoleService revokeOrganizerRoleService;

    @BeforeEach
    void setUp() {
        adminViewMapper = new AdminViewMapper();
        grantOrganizerRoleService = new GrantOrganizerRoleService(
                userRepository,
                roleRepository,
                auditLogRepository,
                adminViewMapper,
                clock);
        revokeOrganizerRoleService = new RevokeOrganizerRoleService(
                userRepository,
                auditLogRepository,
                adminViewMapper,
                clock);
    }

    @Test
    void grantsOrganizerRoleAndWritesAuditLog() {
        User admin = TestEntityFactory.user("admin@example.com");
        User user = TestEntityFactory.user("sofi@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Role organizerRole = new Role(RoleName.ROLE_ORGANIZER);
        Instant now = Instant.parse("2026-06-23T10:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findWithRolesAndProfileById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_ORGANIZER)).thenReturn(Optional.of(organizerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        AdminUserRowView view = grantOrganizerRoleService.grant("admin@example.com", user.getId());

        assertThat(user.hasRole(RoleName.ROLE_ORGANIZER)).isTrue();
        assertThat(view.roles()).contains(RoleName.ROLE_ORGANIZER);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("GRANT_ORGANIZER_ROLE");
    }

    @Test
    void revokesOrganizerRoleAndWritesAuditLog() {
        User admin = TestEntityFactory.user("admin@example.com");
        User user = TestEntityFactory.user("organizer@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.assignRole(new Role(RoleName.ROLE_ORGANIZER));
        Instant now = Instant.parse("2026-06-23T11:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findWithRolesAndProfileById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        AdminUserRowView view = revokeOrganizerRoleService.revoke("admin@example.com", user.getId());

        assertThat(user.hasRole(RoleName.ROLE_ORGANIZER)).isFalse();
        assertThat(view.roles()).doesNotContain(RoleName.ROLE_ORGANIZER);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("REVOKE_ORGANIZER_ROLE");
    }
}
