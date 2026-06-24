package nure.com.eventflow.admin.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.eventflow.admin.application.CancelAdminEventService;
import nure.com.eventflow.admin.application.ListAdminEventsService;
import nure.com.eventflow.admin.application.ModerateEventService;
import nure.com.eventflow.admin.application.ModerationCommand;
import nure.com.eventflow.admin.application.PublishAdminEventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminEventController {

    private final ListAdminEventsService listAdminEventsService;
    private final PublishAdminEventService publishAdminEventService;
    private final CancelAdminEventService cancelAdminEventService;
    private final ModerateEventService moderateEventService;

    public AdminEventController(
            ListAdminEventsService listAdminEventsService,
            PublishAdminEventService publishAdminEventService,
            CancelAdminEventService cancelAdminEventService,
            ModerateEventService moderateEventService
    ) {
        this.listAdminEventsService = listAdminEventsService;
        this.publishAdminEventService = publishAdminEventService;
        this.cancelAdminEventService = cancelAdminEventService;
        this.moderateEventService = moderateEventService;
    }

    @GetMapping("/admin/events")
    String list(Model model) {
        model.addAttribute("events", listAdminEventsService.list());
        return "admin/events/list";
    }

    @PostMapping("/admin/events/{eventId}/publish")
    String publish(
            Principal principal,
            @PathVariable UUID eventId,
            RedirectAttributes redirectAttributes
    ) {
        publishAdminEventService.publish(principal.getName(), eventId);
        redirectAttributes.addFlashAttribute("successMessage", "Event published.");
        return "redirect:/admin/events";
    }

    @PostMapping("/admin/events/{eventId}/cancel")
    String cancel(
            Principal principal,
            @PathVariable UUID eventId,
            RedirectAttributes redirectAttributes
    ) {
        cancelAdminEventService.cancel(principal.getName(), eventId);
        redirectAttributes.addFlashAttribute("successMessage", "Event cancelled.");
        return "redirect:/admin/events";
    }

    @PostMapping("/admin/events/{eventId}/moderate")
    String moderate(
            Principal principal,
            @PathVariable UUID eventId,
            @Valid @ModelAttribute("moderationForm") ModerationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Moderation action is invalid.");
            return "redirect:/admin/events";
        }
        moderateEventService.moderate(
                principal.getName(),
                eventId,
                new ModerationCommand(form.getAction(), form.getReason()));
        redirectAttributes.addFlashAttribute("successMessage", "Moderation action recorded.");
        return "redirect:/admin/events";
    }
}
