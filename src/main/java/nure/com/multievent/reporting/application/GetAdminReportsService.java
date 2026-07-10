package nure.com.multievent.reporting.application;

import nure.com.multievent.reporting.infra.ReportingQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAdminReportsService {

    private final ReportingQueryRepository reportingQueryRepository;

    public GetAdminReportsService(ReportingQueryRepository reportingQueryRepository) {
        this.reportingQueryRepository = reportingQueryRepository;
    }

    @Transactional(readOnly = true)
    public AdminKpiView get() {
        return reportingQueryRepository.adminKpis();
    }
}
