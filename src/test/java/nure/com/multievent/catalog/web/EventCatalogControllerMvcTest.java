package nure.com.multievent.catalog.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.catalog.application.CategoryOptionView;
import nure.com.multievent.catalog.application.EventCardView;
import nure.com.multievent.catalog.application.EventDetailsView;
import nure.com.multievent.catalog.application.GetPublishedEventDetailsService;
import nure.com.multievent.catalog.application.ListPublicCategoriesService;
import nure.com.multievent.catalog.application.SearchPublishedEventsService;
import nure.com.multievent.ticketing.application.TicketOptionView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventCatalogController.class)
@Import({EventCatalogWebMapper.class, SecurityConfig.class})
class EventCatalogControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchPublishedEventsService searchPublishedEventsService;

    @MockitoBean
    private GetPublishedEventDetailsService getPublishedEventDetailsService;

    @MockitoBean
    private ListPublicCategoriesService listPublicCategoriesService;

    @Test
    void showsEventsList() throws Exception {
        when(listPublicCategoriesService.list()).thenReturn(List.of(new CategoryOptionView(UUID.randomUUID(), "Concerts", "concerts")));
        when(searchPublishedEventsService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(eventCardView())));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/list"))
                .andExpect(model().attributeExists("events", "categories", "searchForm"));
    }

    @Test
    void showsEventDetails() throws Exception {
        when(getPublishedEventDetailsService.getBySlug("rock-night")).thenReturn(eventDetailsView());

        mockMvc.perform(get("/events/rock-night"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/details"))
                .andExpect(model().attributeExists("event"));
    }

    private EventCardView eventCardView() {
        return new EventCardView(UUID.randomUUID(), "Rock Night", "rock-night", "Short", "Concerts", "Kyiv", Instant.now(), Instant.now(), "https://example.com/cover.jpg");
    }

    private EventDetailsView eventDetailsView() {
        return new EventDetailsView(
                UUID.randomUUID(),
                "Rock Night",
                "rock-night",
                "Short",
                "Full",
                "Concerts",
                "Palace",
                "Kyiv",
                "Main street 1",
                "01001",
                "Ukraine",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                "organizer@example.com",
                "https://example.com/cover.jpg",
                List.of("https://example.com/cover.jpg"),
                List.of(new TicketOptionView(UUID.randomUUID(), "Standard", "Entry ticket", java.math.BigDecimal.valueOf(25), "USD", 4, 20, true, "On sale")));
    }
}
