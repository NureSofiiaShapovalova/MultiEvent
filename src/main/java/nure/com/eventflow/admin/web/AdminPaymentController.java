package nure.com.eventflow.admin.web;

import nure.com.eventflow.admin.application.ListPaymentIssuesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPaymentController {

    private final ListPaymentIssuesService listPaymentIssuesService;

    public AdminPaymentController(ListPaymentIssuesService listPaymentIssuesService) {
        this.listPaymentIssuesService = listPaymentIssuesService;
    }

    @GetMapping("/admin/payments/issues")
    String issues(Model model) {
        model.addAttribute("payments", listPaymentIssuesService.list());
        return "admin/payments/issues";
    }
}
