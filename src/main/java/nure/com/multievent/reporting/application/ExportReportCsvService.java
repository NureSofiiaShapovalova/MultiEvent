package nure.com.multievent.reporting.application;

import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportReportCsvService {

    private final GetAdminReportsService getAdminReportsService;
    private final GetOrganizerReportsService getOrganizerReportsService;

    public ExportReportCsvService(
            GetAdminReportsService getAdminReportsService,
            GetOrganizerReportsService getOrganizerReportsService
    ) {
        this.getAdminReportsService = getAdminReportsService;
        this.getOrganizerReportsService = getOrganizerReportsService;
    }

    @Transactional(readOnly = true)
    public String exportAdmin(String reportCode) {
        if (!"admin-kpis".equals(reportCode)) {
            throw new NotFoundException("Report was not found.");
        }
        AdminKpiView report = getAdminReportsService.get();
        StringBuilder csv = new StringBuilder();
        csv.append("metric,value\n");
        csv.append(metric("totalUsers", report.totalUsers()));
        csv.append(metric("organizers", report.organizers()));
        csv.append(metric("totalEvents", report.totalEvents()));
        csv.append(metric("publishedEvents", report.publishedEvents()));
        csv.append(metric("pendingOrders", report.pendingOrders()));
        csv.append(metric("paidOrders", report.paidOrders()));
        csv.append(metric("failedPayments", report.failedPayments()));
        csv.append(metric("grossRevenue", report.grossRevenue() + " " + report.currency()));
        csv.append("\ncategory,eventCount,paidOrderCount\n");
        report.categoryPopularity().forEach(row -> csv
                .append(escape(row.categoryName())).append(',')
                .append(row.eventCount()).append(',')
                .append(row.paidOrderCount()).append('\n'));
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportOrganizer(String organizerEmail, String reportCode) {
        if (!"organizer-revenue".equals(reportCode)) {
            throw new NotFoundException("Report was not found.");
        }
        OrganizerReportsView report = getOrganizerReportsService.get(organizerEmail);
        StringBuilder csv = new StringBuilder();
        csv.append("eventId,eventTitle,paidOrders,ticketsSold,revenue,currency\n");
        report.revenueRows().forEach(row -> csv
                .append(row.eventId()).append(',')
                .append(escape(row.eventTitle())).append(',')
                .append(row.paidOrders()).append(',')
                .append(row.ticketsSold()).append(',')
                .append(row.revenue()).append(',')
                .append(row.currency()).append('\n'));
        return csv.toString();
    }

    private static String metric(String name, Object value) {
        return name + "," + escape(String.valueOf(value)) + "\n";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
