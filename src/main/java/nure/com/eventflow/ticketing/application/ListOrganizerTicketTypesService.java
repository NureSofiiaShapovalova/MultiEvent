package nure.com.eventflow.ticketing.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListOrganizerTicketTypesService {

    private final UserRepository userRepository;
    private final TicketingQueryRepository ticketingQueryRepository;
    private final TicketingViewMapper ticketingViewMapper;
    private final Clock clock;

    public ListOrganizerTicketTypesService(
            UserRepository userRepository,
            TicketingQueryRepository ticketingQueryRepository,
            TicketingViewMapper ticketingViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.ticketingViewMapper = ticketingViewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrganizerTicketRowView> list(String organizerEmail, UUID eventId) {
        User organizer = findOrganizer(organizerEmail);
        var now = clock.instant();
        return ticketingQueryRepository.findOrganizerTicketTypes(organizer.getId(), eventId)
                .stream()
                .map(ticketType -> ticketingViewMapper.toOrganizerRowView(ticketType, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizerTicketRowView getForEdit(String organizerEmail, UUID eventId, UUID ticketTypeId) {
        User organizer = findOrganizer(organizerEmail);
        var now = clock.instant();
        return ticketingQueryRepository.findOrganizerTicketType(organizer.getId(), eventId, ticketTypeId)
                .map(ticketType -> ticketingViewMapper.toOrganizerRowView(ticketType, now))
                .orElseThrow(() -> new NotFoundException("Ticket type was not found."));
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = EmailNormalizer.normalize(organizerEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
