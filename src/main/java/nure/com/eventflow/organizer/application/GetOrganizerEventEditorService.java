package nure.com.eventflow.organizer.application;

import java.util.UUID;
import nure.com.eventflow.catalog.application.CategoryOptionView;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.EventOwnershipPolicy;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrganizerEventEditorService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final EventOwnershipPolicy eventOwnershipPolicy;
    private final OrganizerViewMapper organizerViewMapper;

    public GetOrganizerEventEditorService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            OrganizerEventRepository organizerEventRepository,
            EventOwnershipPolicy eventOwnershipPolicy,
            OrganizerViewMapper organizerViewMapper
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.eventOwnershipPolicy = eventOwnershipPolicy;
        this.organizerViewMapper = organizerViewMapper;
    }

    @Transactional(readOnly = true)
    public EventEditorView getForCreate(String organizerEmail) {
        findOrganizer(organizerEmail);
        return organizerViewMapper.toEmptyEditorView(organizerViewMapper.toCategoryOptions(categoryRepository.findAllByOrderByNameAsc()));
    }

    @Transactional(readOnly = true)
    public EventEditorView getForEdit(String organizerEmail, UUID eventId) {
        User organizer = findOrganizer(organizerEmail);
        var event = organizerEventRepository.findByIdAndOrganizer_Id(eventId, organizer.getId())
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        eventOwnershipPolicy.assertOwnedBy(organizer.getId(), event);
        return organizerViewMapper.toEditorView(
                event,
                organizerViewMapper.toCategoryOptions(categoryRepository.findAllByOrderByNameAsc()));
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = organizerEmail == null ? null : organizerEmail.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
