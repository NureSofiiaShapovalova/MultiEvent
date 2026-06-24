package nure.com.eventflow.admin.web;

import nure.com.eventflow.admin.application.GetAdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final GetAdminDashboardService getAdminDashboardService;

    public AdminDashboardController(GetAdminDashboardService getAdminDashboardService) {
        this.getAdminDashboardService = getAdminDashboardService;
    }

    @GetMapping("/admin")
    String dashboard(Model model) {
        model.addAttribute("dashboard", getAdminDashboardService.get());
        return "admin/dashboard";
    }
}
