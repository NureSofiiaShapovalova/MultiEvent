package nure.com.eventflow.checkout.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.eventflow.checkout.application.CheckoutForm;
import nure.com.eventflow.checkout.application.CheckoutSummaryView;
import nure.com.eventflow.checkout.application.CheckoutWebMapper;
import nure.com.eventflow.checkout.application.CreateOrderFromSelectionService;
import nure.com.eventflow.checkout.application.GetCheckoutSummaryService;
import nure.com.eventflow.checkout.application.StartCheckoutService;
import nure.com.eventflow.checkout.application.TicketSelectionCommand;
import nure.com.eventflow.catalog.application.EventDetailsView;
import nure.com.eventflow.payment.application.StartPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

    private final StartCheckoutService startCheckoutService;
    private final CreateOrderFromSelectionService createOrderFromSelectionService;
    private final GetCheckoutSummaryService getCheckoutSummaryService;
    private final StartPaymentService startPaymentService;
    private final CheckoutWebMapper checkoutWebMapper;

    public CheckoutController(
            StartCheckoutService startCheckoutService,
            CreateOrderFromSelectionService createOrderFromSelectionService,
            GetCheckoutSummaryService getCheckoutSummaryService,
            StartPaymentService startPaymentService,
            CheckoutWebMapper checkoutWebMapper
    ) {
        this.startCheckoutService = startCheckoutService;
        this.createOrderFromSelectionService = createOrderFromSelectionService;
        this.getCheckoutSummaryService = getCheckoutSummaryService;
        this.startPaymentService = startPaymentService;
        this.checkoutWebMapper = checkoutWebMapper;
    }

    @GetMapping("/events/{slug}/checkout")
    String checkout(@PathVariable String slug, Model model) {
        EventDetailsView event = startCheckoutService.getBySlug(slug);
        model.addAttribute("event", event);
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", checkoutWebMapper.toForm(event.ticketOptions()));
        }
        return "checkout/select-tickets";
    }

    @PostMapping("/events/{slug}/checkout")
    String createOrder(
            Principal principal,
            @PathVariable String slug,
            @Valid @ModelAttribute("checkoutForm") CheckoutForm checkoutForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            EventDetailsView event = startCheckoutService.getBySlug(slug);
            model.addAttribute("event", event);
            checkoutForm.setSelections(checkoutWebMapper.mergeForm(event.ticketOptions(), checkoutForm).getSelections());
            return "checkout/select-tickets";
        }
        TicketSelectionCommand command = checkoutWebMapper.toSelectionCommand(checkoutForm);
        UUID orderId = createOrderFromSelectionService.create(principal.getName(), slug, command);
        redirectAttributes.addFlashAttribute("successMessage", "Order created.");
        return "redirect:/checkout/orders/" + orderId + "/summary";
    }

    @GetMapping("/checkout/orders/{orderId}/summary")
    String summary(Principal principal, @PathVariable UUID orderId, Model model) {
        CheckoutSummaryView summary = getCheckoutSummaryService.getById(principal.getName(), orderId);
        model.addAttribute("summary", summary);
        return "checkout/summary";
    }

    @PostMapping("/checkout/orders/{orderId}/confirm")
    String startPayment(Principal principal, @PathVariable UUID orderId, RedirectAttributes redirectAttributes) {
        startPaymentService.start(principal.getName(), orderId);
        redirectAttributes.addFlashAttribute("successMessage", "Payment started.");
        return "redirect:/checkout/orders/" + orderId + "/payment";
    }
}
