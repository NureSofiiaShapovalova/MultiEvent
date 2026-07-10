package nure.com.multievent.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportReportCsvServiceTest {

    @Mock
    private GetAdminReportsService getAdminReportsService;

    @Mock
    private GetOrganizerReportsService getOrganizerReportsService;

    @Test
    void exportsOrganizerRevenueAsCsv() {
        UUID eventId = UUID.randomUUID();
        when(getOrganizerReportsService.get("organizer@example.com")).thenReturn(new OrganizerReportsView(
                1,
                2,
                new BigDecimal("50.00"),
                "USD",
                0,
                List.of(new OrganizerRevenueRowView(eventId, "Rock, Night", 1, 2, new BigDecimal("50.00"), "USD"))));
        ExportReportCsvService service = new ExportReportCsvService(getAdminReportsService, getOrganizerReportsService);

        String csv = service.exportOrganizer("organizer@example.com", "organizer-revenue");

        assertThat(csv).contains("eventId,eventTitle,paidOrders,ticketsSold,revenue,currency");
        assertThat(csv).contains("\"Rock, Night\"");
    }
}
