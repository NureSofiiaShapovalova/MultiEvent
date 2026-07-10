package nure.com.multievent.organizer.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.catalog.application.CategoryOptionView;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.organizer.application.CreateEventService;
import nure.com.multievent.organizer.application.EventEditorView;
import nure.com.multievent.organizer.application.GetOrganizerEventEditorService;
import nure.com.multievent.organizer.application.ListOrganizerEventsService;
import nure.com.multievent.organizer.application.OrganizerEventForm;
import nure.com.multievent.organizer.application.OrganizerEventRowView;
import nure.com.multievent.organizer.application.OrganizerViewMapper;
import nure.com.multievent.organizer.application.PublishEventService;
import nure.com.multievent.organizer.application.UpdateEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizerEventController.class)
@Import(SecurityConfig.class)
class OrganizerEventControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListOrganizerEventsService listOrganizerEventsService;

    @MockitoBean
    private GetOrganizerEventEditorService getOrganizerEventEditorService;

    @MockitoBean
    private CreateEventService createEventService;

    @MockitoBean
    private UpdateEventService updateEventService;

    @MockitoBean
    private PublishEventService publishEventService;

    @MockitoBean
    private nure.com.multievent.organizer.application.CancelEventService cancelEventService;

    @MockitoBean
    private OrganizerViewMapper organizerViewMapper;

    @Test
    void showsOrganizerEventsList() throws Exception {
        when(listOrganizerEventsService.list(org.mockito.ArgumentMatchers.anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(new OrganizerEventRowView(
                        UUID.randomUUID(),
                        "Rock Night",
                        "rock-night",
                        EventStatus.DRAFT,
                        "Concerts",
                        "Kyiv",
                        Instant.now(),
                        Instant.now(),
                        "https://example.com/cover.jpg"))));

        mockMvc.perform(get("/organizer/events").with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/events/list"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void showsCreateForm() throws Exception {
        EventEditorView editorView = new EventEditorView(
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                List.of(),
                EventStatus.DRAFT,
                List.of(new CategoryOptionView(UUID.randomUUID(), "Concerts", "concerts")));
        when(getOrganizerEventEditorService.getForCreate("organizer@example.com")).thenReturn(editorView);
        when(organizerViewMapper.toForm(editorView)).thenReturn(new OrganizerEventForm());

        mockMvc.perform(get("/organizer/events/new").with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/events/create"))
                .andExpect(model().attributeExists("eventForm", "categories"));
    }

    @Test
    void rejectsInvalidCreatePayload() throws Exception {
        EventEditorView editorView = new EventEditorView(
                null,
                "",
                "",
                "",
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                List.of(),
                EventStatus.DRAFT,
                List.of(new CategoryOptionView(UUID.randomUUID(), "Concerts", "concerts")));
        when(getOrganizerEventEditorService.getForCreate("organizer@example.com")).thenReturn(editorView);

        mockMvc.perform(post("/organizer/events")
                        .with(user("organizer@example.com").roles("ORGANIZER"))
                        .with(csrf())
                        .param("title", "")
                        .param("slug", "")
                        .param("shortDescription", "")
                        .param("fullDescription", "")
                        .param("categoryId", "")
                        .param("venueName", "")
                        .param("city", "")
                        .param("addressLine", "")
                        .param("postalCode", "")
                        .param("country", "")
                        .param("startsAt", "")
                        .param("endsAt", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/events/create"))
                .andExpect(model().attributeHasFieldErrors("eventForm", "title", "slug", "shortDescription", "fullDescription", "categoryId", "venueName", "city", "addressLine", "country", "startsAt", "endsAt"));
    }
}
