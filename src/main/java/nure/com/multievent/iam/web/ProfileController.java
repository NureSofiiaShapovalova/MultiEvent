package nure.com.multievent.iam.web;

import jakarta.validation.Valid;
import java.security.Principal;
import nure.com.multievent.iam.application.ChangePasswordService;
import nure.com.multievent.iam.application.CurrentUserView;
import nure.com.multievent.iam.application.FindCurrentUserService;
import nure.com.multievent.iam.application.UpdateProfileService;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final FindCurrentUserService findCurrentUserService;
    private final UpdateProfileService updateProfileService;
    private final ChangePasswordService changePasswordService;
    private final ProfileWebMapper profileWebMapper;

    public ProfileController(
            FindCurrentUserService findCurrentUserService,
            UpdateProfileService updateProfileService,
            ChangePasswordService changePasswordService,
            ProfileWebMapper profileWebMapper
    ) {
        this.findCurrentUserService = findCurrentUserService;
        this.updateProfileService = updateProfileService;
        this.changePasswordService = changePasswordService;
        this.profileWebMapper = profileWebMapper;
    }

    @GetMapping("/profile")
    String viewProfile(Principal principal, Model model) {
        CurrentUserView currentUser = findCurrentUserService.findByEmail(principal.getName());
        model.addAttribute("currentUser", currentUser);
        return "profile/view";
    }

    @GetMapping("/profile/edit")
    String editProfile(Principal principal, Model model) {
        if (!model.containsAttribute("profileEditForm")) {
            CurrentUserView currentUser = findCurrentUserService.findByEmail(principal.getName());
            model.addAttribute("profileEditForm", profileWebMapper.toEditForm(currentUser));
        }
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    String updateProfile(
            Principal principal,
            @Valid @ModelAttribute("profileEditForm") ProfileEditForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "profile/edit";
        }

        updateProfileService.updateByEmail(principal.getName(), profileWebMapper.toCommand(form));
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/profile";
    }

    @GetMapping("/profile/password")
    String changePasswordForm(Model model) {
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        return "profile/change-password";
    }

    @PostMapping("/profile/password")
    String changePassword(
            Principal principal,
            @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasFieldErrors("confirmPassword") && !form.passwordsMatch()) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "profile.password.confirmMismatch",
                    "New password and confirmation do not match.");
        }
        if (bindingResult.hasErrors()) {
            return "profile/change-password";
        }

        try {
            changePasswordService.changeByEmail(principal.getName(), profileWebMapper.toCommand(form));
        } catch (BusinessRuleViolationException exception) {
            bindingResult.rejectValue(
                    "currentPassword",
                    "profile.password.currentInvalid",
                    "Current password is incorrect.");
            return "profile/change-password";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Password changed.");
        return "redirect:/profile";
    }
}
