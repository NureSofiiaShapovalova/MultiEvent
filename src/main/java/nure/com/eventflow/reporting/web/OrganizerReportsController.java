package nure.com.eventflow.reporting.web;

import java.security.Principal;
import nure.com.eventflow.reporting.application.ExportReportCsvService;
import nure.com.eventflow.reporting.application.GetOrganizerReportsService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrganizerReportsController {

    private final GetOrganizerReportsService getOrganizerReportsService;
    private final ExportReportCsvService exportReportCsvService;

    public OrganizerReportsController(
            GetOrganizerReportsService getOrganizerReportsService,
            ExportReportCsvService exportReportCsvService
    ) {
        this.getOrganizerReportsService = getOrganizerReportsService;
        this.exportReportCsvService = exportReportCsvService;
    }

    @GetMapping("/organizer/reports")
    String dashboard(Principal principal, Model model) {
        model.addAttribute("report", getOrganizerReportsService.get(principal.getName()));
        return "organizer/reports/dashboard";
    }

    @GetMapping("/organizer/reports/export/{reportCode}")
    ResponseEntity<String> export(Principal principal, @PathVariable String reportCode) {
        return csv(reportCode + ".csv", exportReportCsvService.exportOrganizer(principal.getName(), reportCode));
    }

    private static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
