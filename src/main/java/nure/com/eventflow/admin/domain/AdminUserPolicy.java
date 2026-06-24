package nure.com.eventflow.admin.domain;

import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class AdminUserPolicy {

    public void assertCanLock(User admin, User target) {
        assertDifferentUser(admin, target, "Admins cannot lock their own account.");
    }

    public void assertCanUnlock(User admin, User target) {
        assertDifferentUser(admin, target, "Admins cannot unlock their own account.");
    }

    private static void assertDifferentUser(User admin, User target, String message) {
        if (admin == null || target == null || admin.getId() == null || target.getId() == null) {
            throw new BusinessRuleViolationException("Admin and target user must exist.");
        }
        if (admin.getId().equals(target.getId())) {
            throw new BusinessRuleViolationException(message);
        }
    }
}
