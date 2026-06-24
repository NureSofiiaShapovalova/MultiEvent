package nure.com.eventflow.reporting.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.reporting.application.AdminKpiView;
import nure.com.eventflow.reporting.application.CategoryPopularityRowView;
import nure.com.eventflow.reporting.application.ExportReportCsvService;
import nure.com.eventflow.reporting.application.GetAdminReportsService;
import nure.com.eventflow.reporting.application.GetOrganizerReportsService;
import nure.com.eventflow.reporting.application.OrganizerReportsView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminReportsController.class, OrganizerReportsController.class})
@Import(SecurityConfig.class)
class ReportingControllersMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminReportsService getAdminReportsService;

    @MockitoBean
    private GetOrganizerReportsService getOrganizerReportsService;

    @MockitoBean
    private ExportReportCsvService exportReportCsvService;

    @Test
    void showsAdminReportsForAdminsOnly() throws Exception {
        when(getAdminReportsService.get()).thenReturn(new AdminKpiView(
                5,
                2,
                3,
                1,
                4,
                6,
                1,
                new BigDecimal("120.00"),
                "USD",
                List.of(new CategoryPopularityRowView("Concerts", 3, 6))));

        mockMvc.perform(get("/admin/reports").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/dashboard"))
                .andExpect(model().attributeExists("report"));

        mockMvc.perform(get("/admin/reports").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void showsOrganizerReportsAndExportsCsv() throws Exception {
        when(getOrganizerReportsService.get("organizer@example.com")).thenReturn(new OrganizerReportsView(
                1,
                2,
                new BigDecimal("50.00"),
                "USD",
                0,
                List.of()));
        when(exportReportCsvService.exportOrganizer("organizer@example.com", "organizer-revenue"))
                .thenReturn("eventId,eventTitle,paidOrders,ticketsSold,revenue,currency\n");

        mockMvc.perform(get("/organizer/reports").with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/reports/dashboard"))
                .andExpect(model().attributeExists("report"));

        mockMvc.perform(get("/organizer/reports/export/organizer-revenue").with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));
    }
}
