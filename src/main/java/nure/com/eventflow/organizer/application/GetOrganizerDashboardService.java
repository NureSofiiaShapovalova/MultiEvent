package nure.com.eventflow.organizer.application;

import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrganizerDashboardService {

    private final UserRepository userRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final OrganizerViewMapper organizerViewMapper;

    public GetOrganizerDashboardService(
            UserRepository userRepository,
            OrganizerEventRepository organizerEventRepository,
            OrganizerViewMapper organizerViewMapper
    ) {
        this.userRepository = userRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.organizerViewMapper = organizerViewMapper;
    }

    @Transactional(readOnly = true)
    public OrganizerDashboardView get(String organizerEmail) {
        User organizer = findOrganizer(organizerEmail);
        java.util.UUID organizerId = organizer.getId();
        long totalEvents = organizerEventRepository.countByOrganizer_Id(organizerId);
        long draftEvents = organizerEventRepository.countByOrganizer_IdAndStatus(organizerId, EventStatus.DRAFT);
        long publishedEvents = organizerEventRepository.countByOrganizer_IdAndStatus(organizerId, EventStatus.PUBLISHED);
        long cancelledEvents = organizerEventRepository.countByOrganizer_IdAndStatus(organizerId, EventStatus.CANCELLED);
        return organizerViewMapper.toDashboardView(
                organizer.getEmail(),
                totalEvents,
                draftEvents,
                publishedEvents,
                cancelledEvents,
                organizerEventRepository.findTop5ByOrganizer_IdOrderByCreatedAtDesc(organizerId));
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = organizerEmail == null ? null : organizerEmail.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
