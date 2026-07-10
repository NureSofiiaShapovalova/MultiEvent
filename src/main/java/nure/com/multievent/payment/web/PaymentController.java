package nure.com.multievent.payment.web;

import java.security.Principal;
import java.util.UUID;
import nure.com.multievent.payment.application.ConfirmPaymentService;
import nure.com.multievent.payment.application.PaymentPageView;
import nure.com.multievent.payment.application.RetryPaymentService;
import nure.com.multievent.payment.application.StartPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout/orders/{orderId}/payment")
public class PaymentController {

    private final StartPaymentService startPaymentService;
    private final ConfirmPaymentService confirmPaymentService;
    private final RetryPaymentService retryPaymentService;

    public PaymentController(
            StartPaymentService startPaymentService,
            ConfirmPaymentService confirmPaymentService,
            RetryPaymentService retryPaymentService
    ) {
        this.startPaymentService = startPaymentService;
        this.confirmPaymentService = confirmPaymentService;
        this.retryPaymentService = retryPaymentService;
    }

    @GetMapping
    String start(Principal principal, @PathVariable UUID orderId, Model model) {
        PaymentPageView payment = startPaymentService.start(principal.getName(), orderId);
        model.addAttribute("payment", payment);
        return "payment/start";
    }

    @GetMapping("/result")
    String result(Principal principal, @PathVariable UUID orderId, Model model) {
        PaymentPageView payment = startPaymentService.start(principal.getName(), orderId);
        model.addAttribute("payment", payment);
        return "payment/result";
    }

    @PostMapping("/confirm")
    String confirm(Principal principal, @PathVariable UUID orderId, RedirectAttributes redirectAttributes) {
        PaymentPageView payment = confirmPaymentService.confirm(principal.getName(), orderId);
        if (payment.canConfirm()) {
            redirectAttributes.addFlashAttribute("successMessage", "Payment completed.");
        } else if (payment.canRetry()) {
            redirectAttributes.addFlashAttribute("errorMessage", payment.message());
        } else {
            redirectAttributes.addFlashAttribute("successMessage", payment.message());
        }
        return "redirect:/checkout/orders/" + orderId + "/payment/result";
    }

    @GetMapping("/paypal/return")
    String paypalReturn(Principal principal, @PathVariable UUID orderId, RedirectAttributes redirectAttributes) {
        PaymentPageView payment = confirmPaymentService.confirm(principal.getName(), orderId);
        if ("PAID".equals(payment.paymentStatus())) {
            redirectAttributes.addFlashAttribute("successMessage", "Payment completed.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", payment.message());
        }
        return "redirect:/checkout/orders/" + orderId + "/payment/result";
    }

    @GetMapping("/paypal/cancel")
    String paypalCancel(@PathVariable UUID orderId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "PayPal payment was cancelled.");
        return "redirect:/checkout/orders/" + orderId + "/payment";
    }

    @PostMapping("/retry")
    String retry(Principal principal, @PathVariable UUID orderId, RedirectAttributes redirectAttributes) {
        retryPaymentService.retry(principal.getName(), orderId);
        redirectAttributes.addFlashAttribute("successMessage", "Payment restarted.");
        return "redirect:/checkout/orders/" + orderId + "/payment";
    }
}
