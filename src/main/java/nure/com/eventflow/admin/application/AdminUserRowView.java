package nure.com.eventflow.admin.application;

import java.util.Set;
import java.util.UUID;
import nure.com.eventflow.iam.domain.RoleName;

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
