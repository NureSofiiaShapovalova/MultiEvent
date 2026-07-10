package nure.com.multievent.admin.web;

import java.security.Principal;
import java.util.UUID;
import nure.com.multievent.admin.application.GrantOrganizerRoleService;
import nure.com.multievent.admin.application.ListAdminUsersService;
import nure.com.multievent.admin.application.RevokeOrganizerRoleService;
import nure.com.multievent.admin.application.ToggleUserLockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminUserController {

    private final ListAdminUsersService listAdminUsersService;
    private final GrantOrganizerRoleService grantOrganizerRoleService;
    private final RevokeOrganizerRoleService revokeOrganizerRoleService;
    private final ToggleUserLockService toggleUserLockService;

    public AdminUserController(
            ListAdminUsersService listAdminUsersService,
            GrantOrganizerRoleService grantOrganizerRoleService,
            RevokeOrganizerRoleService revokeOrganizerRoleService,
            ToggleUserLockService toggleUserLockService
    ) {
        this.listAdminUsersService = listAdminUsersService;
        this.grantOrganizerRoleService = grantOrganizerRoleService;
        this.revokeOrganizerRoleService = revokeOrganizerRoleService;
        this.toggleUserLockService = toggleUserLockService;
    }

    @GetMapping("/admin/users")
    String list(Model model) {
        model.addAttribute("users", listAdminUsersService.list());
        return "admin/users/list";
    }

    @PostMapping("/admin/users/{userId}/roles/organizer/grant")
    String grantOrganizerRoleLegacy(
            Principal principal,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes
    ) {
        return grantOrganizerRole(principal, userId, redirectAttributes);
    }

    @PostMapping("/admin/users/{userId}/grant-organizer")
    String grantOrganizerRole(
            Principal principal,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes
    ) {
        grantOrganizerRoleService.grant(principal.getName(), userId);
        redirectAttributes.addFlashAttribute("successMessage", "Organizer role granted.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{userId}/roles/organizer/revoke")
    String revokeOrganizerRoleLegacy(
            Principal principal,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes
    ) {
        return revokeOrganizerRole(principal, userId, redirectAttributes);
    }

    @PostMapping("/admin/users/{userId}/revoke-organizer")
    String revokeOrganizerRole(
            Principal principal,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes
    ) {
        revokeOrganizerRoleService.revoke(principal.getName(), userId);
        redirectAttributes.addFlashAttribute("successMessage", "Organizer role revoked.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{userId}/lock")
    String lock(
            Principal principal,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes
    ) {
        toggleUserLockService.lock(principal.getName(), userId);
        redirectAttributes.addFlashAttribute("successMessage", "User account locked.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{userId}/unlock")
    String unlock(
            Principal principal,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes
    ) {
        toggleUserLockService.unlock(principal.getName(), userId);
        redirectAttributes.addFlashAttribute("successMessage", "User account unlocked.");
        return "redirect:/admin/users";
    }
}
