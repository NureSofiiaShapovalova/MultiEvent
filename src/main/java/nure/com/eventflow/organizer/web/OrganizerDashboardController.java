package nure.com.eventflow.organizer.web;

import java.security.Principal;
import nure.com.eventflow.organizer.application.GetOrganizerDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrganizerDashboardController {

    private final GetOrganizerDashboardService getOrganizerDashboardService;

    public OrganizerDashboardController(GetOrganizerDashboardService getOrganizerDashboardService) {
        this.getOrganizerDashboardService = getOrganizerDashboardService;
    }

    @GetMapping("/organizer/dashboard")
    String dashboard(Principal principal, Model model) {
        model.addAttribute("dashboard", getOrganizerDashboardService.get(principal.getName()));
        return "organizer/dashboard";
    }
}
