package nure.com.multievent.notifications.web;

import java.security.Principal;
import nure.com.multievent.notifications.application.NotificationPreferencesCommand;
import nure.com.multievent.notifications.application.NotificationPreferencesView;
import nure.com.multievent.notifications.application.UpdateNotificationPreferencesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationPreferencesController {

    private final UpdateNotificationPreferencesService updateNotificationPreferencesService;

    public NotificationPreferencesController(UpdateNotificationPreferencesService updateNotificationPreferencesService) {
        this.updateNotificationPreferencesService = updateNotificationPreferencesService;
    }

    @GetMapping("/profile/notifications")
    String viewPreferences(Principal principal, Model model) {
        if (!model.containsAttribute("notificationPreferencesForm")) {
            NotificationPreferencesView preferences = updateNotificationPreferencesService.getOrCreate(principal.getName());
            model.addAttribute("notificationPreferencesForm", toForm(preferences));
        }
        return "profile/notifications";
    }

    @PostMapping("/profile/notifications")
    String updatePreferences(
            Principal principal,
            @ModelAttribute("notificationPreferencesForm") NotificationPreferencesForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "profile/notifications";
        }

        updateNotificationPreferencesService.update(
                principal.getName(),
                new NotificationPreferencesCommand(
                        form.isEmailEnabled(),
                        form.isOrderUpdatesEnabled(),
                        form.isMarketingEnabled()));
        redirectAttributes.addFlashAttribute("successMessage", "Notification preferences updated.");
        return "redirect:/profile/notifications";
    }

    private NotificationPreferencesForm toForm(NotificationPreferencesView view) {
        NotificationPreferencesForm form = new NotificationPreferencesForm();
        form.setEmailEnabled(view.emailEnabled());
        form.setOrderUpdatesEnabled(view.orderUpdatesEnabled());
        form.setMarketingEnabled(view.marketingEnabled());
        return form;
    }
}
