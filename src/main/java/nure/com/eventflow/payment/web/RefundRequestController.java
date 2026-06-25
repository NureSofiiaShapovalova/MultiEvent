package nure.com.eventflow.payment.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.eventflow.checkout.application.GetUserOrderDetailsService;
import nure.com.eventflow.payment.application.RequestRefundService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RefundRequestController {

    private final GetUserOrderDetailsService getUserOrderDetailsService;
    private final RequestRefundService requestRefundService;

    public RefundRequestController(
            GetUserOrderDetailsService getUserOrderDetailsService,
            RequestRefundService requestRefundService
    ) {
        this.getUserOrderDetailsService = getUserOrderDetailsService;
        this.requestRefundService = requestRefundService;
    }

    @GetMapping("/orders/{orderId}/refund-request")
    String form(Principal principal, @PathVariable UUID orderId, Model model) {
        model.addAttribute("order", getUserOrderDetailsService.getById(principal.getName(), orderId));
        if (!model.containsAttribute("refundRequestForm")) {
            model.addAttribute("refundRequestForm", new RefundRequestForm());
        }
        return "orders/refund-request";
    }

    @PostMapping("/orders/{orderId}/refund-request")
    String submit(
            Principal principal,
            @PathVariable UUID orderId,
            @Valid @ModelAttribute("refundRequestForm") RefundRequestForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("order", getUserOrderDetailsService.getById(principal.getName(), orderId));
            return "orders/refund-request";
        }

        requestRefundService.request(principal.getName(), orderId, form.getReason());
        redirectAttributes.addFlashAttribute("successMessage", "Refund request submitted for review.");
        return "redirect:/orders/" + orderId;
    }
}
