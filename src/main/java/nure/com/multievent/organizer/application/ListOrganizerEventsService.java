package nure.com.multievent.organizer.application;

import java.util.UUID;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListOrganizerEventsService {

    private final UserRepository userRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final OrganizerViewMapper organizerViewMapper;

    public ListOrganizerEventsService(
            UserRepository userRepository,
            OrganizerEventRepository organizerEventRepository,
            OrganizerViewMapper organizerViewMapper
    ) {
        this.userRepository = userRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.organizerViewMapper = organizerViewMapper;
    }

    @Transactional(readOnly = true)
    public Page<OrganizerEventRowView> list(String organizerEmail, Pageable pageable) {
        User organizer = findOrganizer(organizerEmail);
        return organizerEventRepository.findByOrganizer_IdOrderByCreatedAtDesc(organizer.getId(), pageable)
                .map(organizerViewMapper::toRowView);
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = organizerEmail == null ? null : organizerEmail.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
