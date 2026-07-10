package nure.com.multievent.iam.application;

import java.util.UUID;
import nure.com.multievent.iam.domain.Role;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.RoleRepository;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AssignRoleService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public CurrentUserView assignRole(UUID userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found."));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NotFoundException("Role was not found."));

        user.assignRole(role);
        return CurrentUserView.from(userRepository.save(user));
    }
}
