package nure.com.multievent.iam.web;

import jakarta.validation.Valid;
import java.security.Principal;
import nure.com.multievent.iam.application.CurrentUserView;
import nure.com.multievent.iam.application.FindCurrentUserService;
import nure.com.multievent.iam.application.UpdateProfileService;
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
    private final ProfileWebMapper profileWebMapper;

    public ProfileController(
            FindCurrentUserService findCurrentUserService,
            UpdateProfileService updateProfileService,
            ProfileWebMapper profileWebMapper
    ) {
        this.findCurrentUserService = findCurrentUserService;
        this.updateProfileService = updateProfileService;
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
}
