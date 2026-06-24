package nure.com.eventflow.catalog.web;

import java.util.List;
import nure.com.eventflow.catalog.application.CategoryOptionView;
import nure.com.eventflow.catalog.application.EventCardView;
import nure.com.eventflow.catalog.application.EventDetailsView;
import nure.com.eventflow.catalog.application.GetPublishedEventDetailsService;
import nure.com.eventflow.catalog.application.ListPublicCategoriesService;
import nure.com.eventflow.catalog.application.SearchPublishedEventsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EventCatalogController {

    private final SearchPublishedEventsService searchPublishedEventsService;
    private final GetPublishedEventDetailsService getPublishedEventDetailsService;
    private final ListPublicCategoriesService listPublicCategoriesService;
    private final EventCatalogWebMapper eventCatalogWebMapper;

    public EventCatalogController(
            SearchPublishedEventsService searchPublishedEventsService,
            GetPublishedEventDetailsService getPublishedEventDetailsService,
            ListPublicCategoriesService listPublicCategoriesService,
            EventCatalogWebMapper eventCatalogWebMapper
    ) {
        this.searchPublishedEventsService = searchPublishedEventsService;
        this.getPublishedEventDetailsService = getPublishedEventDetailsService;
        this.listPublicCategoriesService = listPublicCategoriesService;
        this.eventCatalogWebMapper = eventCatalogWebMapper;
    }

    @ModelAttribute("searchForm")
    EventSearchForm searchForm() {
        return new EventSearchForm();
    }

    @GetMapping("/events")
    String list(
            @ModelAttribute("searchForm") EventSearchForm searchForm,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            Model model
    ) {
        Page<EventCardView> events = searchPublishedEventsService.search(
                eventCatalogWebMapper.toCriteria(searchForm),
                PageRequest.of(page, size, toSort(searchForm.getSort())));
        model.addAttribute("events", events);
        model.addAttribute("categories", categories());
        return "events/list";
    }

    @GetMapping("/events/{slug}")
    String details(@PathVariable String slug, Model model) {
        EventDetailsView event = getPublishedEventDetailsService.getBySlug(slug);
        model.addAttribute("event", event);
        return "events/details";
    }

    private List<CategoryOptionView> categories() {
        return listPublicCategoriesService.list();
    }

    private Sort toSort(String sort) {
        if ("startsAtDesc".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "startsAt");
        }
        if ("newest".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return Sort.by(Sort.Direction.ASC, "startsAt");
    }
}
