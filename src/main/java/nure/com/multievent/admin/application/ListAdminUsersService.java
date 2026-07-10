package nure.com.multievent.admin.application;

import java.util.List;
import nure.com.multievent.iam.infra.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAdminUsersService {

    private final UserRepository userRepository;
    private final AdminViewMapper adminViewMapper;

    public ListAdminUsersService(UserRepository userRepository, AdminViewMapper adminViewMapper) {
        this.userRepository = userRepository;
        this.adminViewMapper = adminViewMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminUserRowView> list() {
        return userRepository.findAllByOrderByEmailAsc().stream()
                .map(adminViewMapper::toUserRowView)
                .toList();
    }
}
