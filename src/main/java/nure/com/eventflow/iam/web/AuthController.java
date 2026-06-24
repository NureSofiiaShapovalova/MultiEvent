package nure.com.eventflow.iam.web;

import jakarta.validation.Valid;
import nure.com.eventflow.iam.application.RegisterUserService;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final RegisterUserService registerUserService;
    private final ProfileWebMapper profileWebMapper;

    public AuthController(RegisterUserService registerUserService, ProfileWebMapper profileWebMapper) {
        this.registerUserService = registerUserService;
        this.profileWebMapper = profileWebMapper;
    }

    @GetMapping("/login")
    String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    String register(
            @Valid @ModelAttribute("registerForm") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            registerUserService.register(profileWebMapper.toCommand(form));
        } catch (BusinessRuleViolationException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Registration completed. Please log in.");
        return "redirect:/login";
    }
}
