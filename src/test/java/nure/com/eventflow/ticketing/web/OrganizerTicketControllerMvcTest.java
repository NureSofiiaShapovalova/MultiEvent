package nure.com.eventflow.ticketing.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.ticketing.application.CreateTicketTypeService;
import nure.com.eventflow.ticketing.application.DeactivateTicketTypeService;
import nure.com.eventflow.ticketing.application.ListOrganizerTicketTypesService;
import nure.com.eventflow.ticketing.application.OrganizerTicketRowView;
import nure.com.eventflow.ticketing.application.UpdateTicketTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizerTicketController.class)
@Import({SecurityConfig.class, TicketTypeWebMapper.class})
class OrganizerTicketControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListOrganizerTicketTypesService listOrganizerTicketTypesService;

    @MockitoBean
    private CreateTicketTypeService createTicketTypeService;

    @MockitoBean
    private UpdateTicketTypeService updateTicketTypeService;

    @MockitoBean
    private DeactivateTicketTypeService deactivateTicketTypeService;

    @Test
    void showsTicketListAndCreateForm() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(listOrganizerTicketTypesService.list("organizer@example.com", eventId)).thenReturn(List.of(ticketRow()));

        mockMvc.perform(get("/organizer/events/{eventId}/tickets", eventId).with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/tickets/list"))
                .andExpect(model().attributeExists("ticketTypes"));

        mockMvc.perform(get("/organizer/events/{eventId}/tickets/new", eventId).with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/tickets/create"))
                .andExpect(model().attributeExists("ticketForm"));
    }

    @Test
    void rejectsInvalidCreatePayload() throws Exception {
        UUID eventId = UUID.randomUUID();
        mockMvc.perform(post("/organizer/events/{eventId}/tickets", eventId)
                        .with(user("organizer@example.com").roles("ORGANIZER"))
                        .with(csrf())
                        .param("name", "")
                        .param("description", "")
                        .param("price", "")
                        .param("currency", "")
                        .param("maxPerOrder", "")
                        .param("totalQuantity", "")
                        .param("salesStart", "")
                        .param("salesEnd", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/tickets/create"))
                .andExpect(model().attributeHasFieldErrors("ticketForm", "name", "price", "currency", "maxPerOrder", "totalQuantity", "salesStart", "salesEnd"));
    }

    @Test
    void showsEditForm() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        when(listOrganizerTicketTypesService.getForEdit("organizer@example.com", eventId, ticketTypeId)).thenReturn(ticketRow());

        mockMvc.perform(get("/organizer/events/{eventId}/tickets/{ticketTypeId}/edit", eventId, ticketTypeId)
                        .with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/tickets/edit"))
                .andExpect(model().attributeExists("ticketForm"));
    }

    private OrganizerTicketRowView ticketRow() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        return new OrganizerTicketRowView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Rock Night",
                "Standard",
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                true,
                100,
                10,
                5,
                85,
                "On sale");
    }
}
