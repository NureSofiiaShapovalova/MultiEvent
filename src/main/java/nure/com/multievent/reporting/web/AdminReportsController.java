package nure.com.multievent.reporting.web;

import nure.com.multievent.reporting.application.ExportReportCsvService;
import nure.com.multievent.reporting.application.GetAdminReportsService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminReportsController {

    private final GetAdminReportsService getAdminReportsService;
    private final ExportReportCsvService exportReportCsvService;

    public AdminReportsController(
            GetAdminReportsService getAdminReportsService,
            ExportReportCsvService exportReportCsvService
    ) {
        this.getAdminReportsService = getAdminReportsService;
        this.exportReportCsvService = exportReportCsvService;
    }

    @GetMapping("/admin/reports")
    String dashboard(Model model) {
        model.addAttribute("report", getAdminReportsService.get());
        return "admin/reports/dashboard";
    }

    @GetMapping("/admin/reports/export/{reportCode}")
    ResponseEntity<String> export(@PathVariable String reportCode) {
        return csv(reportCode + ".csv", exportReportCsvService.exportAdmin(reportCode));
    }

    private static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
