package nure.com.eventflow.organizer.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.organizer.application.GetOrganizerDashboardService;
import nure.com.eventflow.organizer.application.OrganizerDashboardView;
import nure.com.eventflow.organizer.application.OrganizerEventRowView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizerDashboardController.class)
@Import(SecurityConfig.class)
class OrganizerDashboardControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetOrganizerDashboardService getOrganizerDashboardService;

    @Test
    void showsDashboardForOrganizer() throws Exception {
        when(getOrganizerDashboardService.get("organizer@example.com")).thenReturn(new OrganizerDashboardView(
                "organizer@example.com",
                3,
                1,
                1,
                1,
                List.of(new OrganizerEventRowView(
                        UUID.randomUUID(),
                        "Rock Night",
                        "rock-night",
                        EventStatus.PUBLISHED,
                        "Concerts",
                        "Kyiv",
                        Instant.now(),
                        Instant.now(),
                        "https://example.com/cover.jpg"))));

        mockMvc.perform(get("/organizer/dashboard").with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/dashboard"))
                .andExpect(model().attributeExists("dashboard"));
    }
}
