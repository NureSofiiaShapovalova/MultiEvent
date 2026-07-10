package nure.com.multievent.admin.web;

import nure.com.multievent.admin.application.ListAuditTrailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminAuditController {

    private final ListAuditTrailService listAuditTrailService;

    public AdminAuditController(ListAuditTrailService listAuditTrailService) {
        this.listAuditTrailService = listAuditTrailService;
    }

    @GetMapping("/admin/audit")
    String audit(Model model) {
        model.addAttribute("auditEntries", listAuditTrailService.list());
        return "admin/audit/list";
    }
}
