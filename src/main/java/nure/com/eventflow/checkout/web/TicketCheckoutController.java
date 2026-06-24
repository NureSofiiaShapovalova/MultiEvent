package nure.com.eventflow.checkout.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.eventflow.checkout.application.AdjustReservationCommand;
import nure.com.eventflow.checkout.application.AdjustTicketReservationService;
import nure.com.eventflow.checkout.application.ConfirmTicketReservationService;
import nure.com.eventflow.checkout.application.CreateTicketReservationService;
import nure.com.eventflow.checkout.application.GetReservationService;
import nure.com.eventflow.checkout.application.GetTicketCheckoutService;
import nure.com.eventflow.checkout.application.ReleaseTicketReservationService;
import nure.com.eventflow.checkout.application.ReservationView;
import nure.com.eventflow.checkout.application.TicketCheckoutForm;
import nure.com.eventflow.checkout.application.TicketCheckoutView;
import nure.com.eventflow.checkout.application.TicketCheckoutWebMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TicketCheckoutController {

    private final GetTicketCheckoutService getTicketCheckoutService;
    private final GetReservationService getReservationService;
    private final CreateTicketReservationService createTicketReservationService;
    private final AdjustTicketReservationService adjustTicketReservationService;
    private final ReleaseTicketReservationService releaseTicketReservationService;
    private final ConfirmTicketReservationService confirmTicketReservationService;
    private final TicketCheckoutWebMapper ticketCheckoutWebMapper;

    public TicketCheckoutController(
            GetTicketCheckoutService getTicketCheckoutService,
            GetReservationService getReservationService,
            CreateTicketReservationService createTicketReservationService,
            AdjustTicketReservationService adjustTicketReservationService,
            ReleaseTicketReservationService releaseTicketReservationService,
            ConfirmTicketReservationService confirmTicketReservationService,
            TicketCheckoutWebMapper ticketCheckoutWebMapper
    ) {
        this.getTicketCheckoutService = getTicketCheckoutService;
        this.getReservationService = getReservationService;
        this.createTicketReservationService = createTicketReservationService;
        this.adjustTicketReservationService = adjustTicketReservationService;
        this.releaseTicketReservationService = releaseTicketReservationService;
        this.confirmTicketReservationService = confirmTicketReservationService;
        this.ticketCheckoutWebMapper = ticketCheckoutWebMapper;
    }

    @GetMapping("/checkout/events/{slug}/tickets/{ticketTypeId}")
    String checkoutForm(@PathVariable String slug, @PathVariable UUID ticketTypeId, Model model) {
        TicketCheckoutView checkoutView = getTicketCheckoutService.getBySlugAndTicketTypeId(slug, ticketTypeId);
        model.addAttribute("checkoutView", checkoutView);
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", ticketCheckoutWebMapper.toForm(checkoutView));
        }
        return "checkout/ticket";
    }

    @PostMapping("/checkout/events/{slug}/tickets/{ticketTypeId}")
    String create(
            Principal principal,
            @PathVariable String slug,
            @PathVariable UUID ticketTypeId,
            @Valid @ModelAttribute("checkoutForm") TicketCheckoutForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("checkoutView", getTicketCheckoutService.getBySlugAndTicketTypeId(slug, ticketTypeId));
            return "checkout/ticket";
        }
        UUID reservationId = createTicketReservationService.create(
                principal.getName(),
                slug,
                ticketCheckoutWebMapper.toCreateCommand(ticketTypeId, form));
        redirectAttributes.addFlashAttribute("successMessage", "Reservation created.");
        return "redirect:/checkout/reservations/" + reservationId;
    }

    @GetMapping("/checkout/reservations/{reservationId}")
    String reservation(Principal principal, @PathVariable UUID reservationId, Model model) {
        ReservationView reservationView = getReservationService.getById(principal.getName(), reservationId);
        model.addAttribute("reservation", reservationView);
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", ticketCheckoutWebMapper.toForm(reservationView));
        }
        return "checkout/reservation";
    }

    @PostMapping("/checkout/reservations/{reservationId}/quantity")
    String adjust(
            Principal principal,
            @PathVariable UUID reservationId,
            @Valid @ModelAttribute("checkoutForm") TicketCheckoutForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("reservation", getReservationService.getById(principal.getName(), reservationId));
            return "checkout/reservation";
        }
        adjustTicketReservationService.adjust(principal.getName(), reservationId, new AdjustReservationCommand(form.getQuantity()));
        redirectAttributes.addFlashAttribute("successMessage", "Reservation updated.");
        return "redirect:/checkout/reservations/" + reservationId;
    }

    @PostMapping("/checkout/reservations/{reservationId}/confirm")
    String confirm(Principal principal, @PathVariable UUID reservationId, RedirectAttributes redirectAttributes) {
        confirmTicketReservationService.confirm(principal.getName(), reservationId);
        redirectAttributes.addFlashAttribute("successMessage", "Reservation confirmed.");
        return "redirect:/checkout/reservations/" + reservationId;
    }

    @PostMapping("/checkout/reservations/{reservationId}/release")
    String release(Principal principal, @PathVariable UUID reservationId, RedirectAttributes redirectAttributes) {
        releaseTicketReservationService.release(principal.getName(), reservationId);
        redirectAttributes.addFlashAttribute("successMessage", "Reservation released.");
        return "redirect:/checkout/reservations/" + reservationId;
    }
}
