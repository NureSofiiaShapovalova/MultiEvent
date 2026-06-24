package nure.com.eventflow.organizer.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.eventflow.organizer.application.CancelEventCommand;
import nure.com.eventflow.organizer.application.CreateEventService;
import nure.com.eventflow.organizer.application.GetOrganizerEventEditorService;
import nure.com.eventflow.organizer.application.ListOrganizerEventsService;
import nure.com.eventflow.organizer.application.OrganizerEventForm;
import nure.com.eventflow.organizer.application.OrganizerViewMapper;
import nure.com.eventflow.organizer.application.PublishEventService;
import nure.com.eventflow.organizer.application.UpdateEventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrganizerEventController {

    private final ListOrganizerEventsService listOrganizerEventsService;
    private final GetOrganizerEventEditorService getOrganizerEventEditorService;
    private final CreateEventService createEventService;
    private final UpdateEventService updateEventService;
    private final PublishEventService publishEventService;
    private final nure.com.eventflow.organizer.application.CancelEventService cancelEventService;
    private final OrganizerViewMapper organizerViewMapper;

    public OrganizerEventController(
            ListOrganizerEventsService listOrganizerEventsService,
            GetOrganizerEventEditorService getOrganizerEventEditorService,
            CreateEventService createEventService,
            UpdateEventService updateEventService,
            PublishEventService publishEventService,
            nure.com.eventflow.organizer.application.CancelEventService cancelEventService,
            OrganizerViewMapper organizerViewMapper
    ) {
        this.listOrganizerEventsService = listOrganizerEventsService;
        this.getOrganizerEventEditorService = getOrganizerEventEditorService;
        this.createEventService = createEventService;
        this.updateEventService = updateEventService;
        this.publishEventService = publishEventService;
        this.cancelEventService = cancelEventService;
        this.organizerViewMapper = organizerViewMapper;
    }

    @GetMapping("/organizer/events")
    String list(Principal principal, Model model) {
        model.addAttribute("events", listOrganizerEventsService.list(
                principal.getName(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "organizer/events/list";
    }

    @GetMapping("/organizer/events/new")
    String createForm(Principal principal, Model model) {
        if (!model.containsAttribute("eventForm")) {
            var editorView = getOrganizerEventEditorService.getForCreate(principal.getName());
            model.addAttribute("eventForm", organizerViewMapper.toForm(editorView));
            model.addAttribute("categories", editorView.categories());
        }
        return "organizer/events/create";
    }

    @PostMapping("/organizer/events")
    String create(
            Principal principal,
            @Valid @ModelAttribute("eventForm") OrganizerEventForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", getOrganizerEventEditorService.getForCreate(principal.getName()).categories());
            return "organizer/events/create";
        }
        createEventService.create(principal.getName(), organizerViewMapper.toCreateCommand(form));
        redirectAttributes.addFlashAttribute("successMessage", "Event created.");
        return "redirect:/organizer/events";
    }

    @GetMapping("/organizer/events/{eventId}/edit")
    String editForm(Principal principal, @PathVariable UUID eventId, Model model) {
        if (!model.containsAttribute("eventForm")) {
            var editorView = getOrganizerEventEditorService.getForEdit(principal.getName(), eventId);
            model.addAttribute("eventForm", organizerViewMapper.toForm(editorView));
            model.addAttribute("categories", editorView.categories());
            model.addAttribute("eventId", eventId);
            model.addAttribute("eventStatus", editorView.status());
        }
        return "organizer/events/edit";
    }

    @PostMapping("/organizer/events/{eventId}")
    String update(
            Principal principal,
            @PathVariable UUID eventId,
        @Valid @ModelAttribute("eventForm") OrganizerEventForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            var editorView = getOrganizerEventEditorService.getForEdit(principal.getName(), eventId);
            model.addAttribute("categories", editorView.categories());
            model.addAttribute("eventId", eventId);
            model.addAttribute("eventStatus", editorView.status());
            return "organizer/events/edit";
        }
        updateEventService.update(principal.getName(), eventId, organizerViewMapper.toUpdateCommand(form));
        redirectAttributes.addFlashAttribute("successMessage", "Event updated.");
        return "redirect:/organizer/events";
    }

    @PostMapping("/organizer/events/{eventId}/publish")
    String publish(
            Principal principal,
            @PathVariable UUID eventId,
            RedirectAttributes redirectAttributes
    ) {
        publishEventService.publish(principal.getName(), eventId);
        redirectAttributes.addFlashAttribute("successMessage", "Event published.");
        return "redirect:/organizer/events";
    }

    @PostMapping("/organizer/events/{eventId}/cancel")
    String cancel(
            Principal principal,
            @PathVariable UUID eventId,
            RedirectAttributes redirectAttributes
    ) {
        cancelEventService.cancel(principal.getName(), eventId, new CancelEventCommand(null));
        redirectAttributes.addFlashAttribute("successMessage", "Event cancelled.");
        return "redirect:/organizer/events";
    }
}
