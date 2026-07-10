package nure.com.multievent.admin.application;

import java.util.Set;
import java.util.UUID;
import nure.com.multievent.iam.domain.RoleName;

public record AdminUserRowView(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String city,
        boolean enabled,
        boolean accountNonLocked,
        Set<RoleName> roles
) {
}
