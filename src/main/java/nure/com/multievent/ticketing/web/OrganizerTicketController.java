package nure.com.multievent.ticketing.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.multievent.ticketing.application.CreateTicketTypeService;
import nure.com.multievent.ticketing.application.DeactivateTicketTypeService;
import nure.com.multievent.ticketing.application.ListOrganizerTicketTypesService;
import nure.com.multievent.ticketing.application.UpdateTicketTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrganizerTicketController {

    private final ListOrganizerTicketTypesService listOrganizerTicketTypesService;
    private final CreateTicketTypeService createTicketTypeService;
    private final UpdateTicketTypeService updateTicketTypeService;
    private final DeactivateTicketTypeService deactivateTicketTypeService;
    private final TicketTypeWebMapper ticketTypeWebMapper;

    public OrganizerTicketController(
            ListOrganizerTicketTypesService listOrganizerTicketTypesService,
            CreateTicketTypeService createTicketTypeService,
            UpdateTicketTypeService updateTicketTypeService,
            DeactivateTicketTypeService deactivateTicketTypeService,
            TicketTypeWebMapper ticketTypeWebMapper
    ) {
        this.listOrganizerTicketTypesService = listOrganizerTicketTypesService;
        this.createTicketTypeService = createTicketTypeService;
        this.updateTicketTypeService = updateTicketTypeService;
        this.deactivateTicketTypeService = deactivateTicketTypeService;
        this.ticketTypeWebMapper = ticketTypeWebMapper;
    }

    @GetMapping("/organizer/events/{eventId}/tickets")
    String list(Principal principal, @PathVariable UUID eventId, Model model) {
        model.addAttribute("ticketTypes", listOrganizerTicketTypesService.list(principal.getName(), eventId));
        model.addAttribute("eventId", eventId);
        return "organizer/tickets/list";
    }

    @GetMapping("/organizer/events/{eventId}/tickets/new")
    String createForm(@PathVariable UUID eventId, Model model) {
        if (!model.containsAttribute("ticketForm")) {
            model.addAttribute("ticketForm", ticketTypeWebMapper.emptyForm());
        }
        model.addAttribute("eventId", eventId);
        return "organizer/tickets/create";
    }

    @PostMapping("/organizer/events/{eventId}/tickets")
    String create(
            Principal principal,
            @PathVariable UUID eventId,
            @Valid @ModelAttribute("ticketForm") TicketTypeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("eventId", eventId);
            return "organizer/tickets/create";
        }
        createTicketTypeService.create(principal.getName(), eventId, ticketTypeWebMapper.toCreateCommand(form));
        redirectAttributes.addFlashAttribute("successMessage", "Ticket type created.");
        return "redirect:/organizer/events/{eventId}/tickets";
    }

    @GetMapping("/organizer/events/{eventId}/tickets/{ticketTypeId}/edit")
    String editForm(
            Principal principal,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketTypeId,
            Model model
    ) {
        if (!model.containsAttribute("ticketForm")) {
            model.addAttribute("ticketForm", ticketTypeWebMapper.toForm(
                    listOrganizerTicketTypesService.getForEdit(principal.getName(), eventId, ticketTypeId)));
        }
        model.addAttribute("eventId", eventId);
        model.addAttribute("ticketTypeId", ticketTypeId);
        return "organizer/tickets/edit";
    }

    @PostMapping("/organizer/events/{eventId}/tickets/{ticketTypeId}")
    String update(
            Principal principal,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketTypeId,
            @Valid @ModelAttribute("ticketForm") TicketTypeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("eventId", eventId);
            model.addAttribute("ticketTypeId", ticketTypeId);
            return "organizer/tickets/edit";
        }
        updateTicketTypeService.update(principal.getName(), eventId, ticketTypeId, ticketTypeWebMapper.toUpdateCommand(form));
        redirectAttributes.addFlashAttribute("successMessage", "Ticket type updated.");
        return "redirect:/organizer/events/{eventId}/tickets";
    }

    @PostMapping("/organizer/events/{eventId}/tickets/{ticketTypeId}/deactivate")
    String deactivate(
            Principal principal,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketTypeId,
            RedirectAttributes redirectAttributes
    ) {
        deactivateTicketTypeService.deactivate(principal.getName(), eventId, ticketTypeId);
        redirectAttributes.addFlashAttribute("successMessage", "Ticket type deactivated.");
        return "redirect:/organizer/events/{eventId}/tickets";
    }
}
