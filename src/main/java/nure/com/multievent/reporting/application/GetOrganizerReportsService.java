package nure.com.multievent.reporting.application;

import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.reporting.infra.ReportingQueryRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrganizerReportsService {

    private final UserRepository userRepository;
    private final ReportingQueryRepository reportingQueryRepository;

    public GetOrganizerReportsService(
            UserRepository userRepository,
            ReportingQueryRepository reportingQueryRepository
    ) {
        this.userRepository = userRepository;
        this.reportingQueryRepository = reportingQueryRepository;
    }

    @Transactional(readOnly = true)
    public OrganizerReportsView get(String organizerEmail) {
        var organizer = userRepository.findByEmail(EmailNormalizer.normalize(organizerEmail))
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
        return reportingQueryRepository.organizerReports(organizer.getId());
    }
}
