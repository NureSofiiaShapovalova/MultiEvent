package nure.com.multievent.ticketing.application;

import java.util.UUID;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.shared.exception.NotFoundException;
import nure.com.multievent.ticketing.domain.TicketSalesPolicy;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
import nure.com.multievent.ticketing.infra.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateTicketTypeService {

    private final UserRepository userRepository;
    private final TicketingQueryRepository ticketingQueryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final AuditLogRepository auditLogRepository;
    private final TicketSalesPolicy ticketSalesPolicy;

    public DeactivateTicketTypeService(
            UserRepository userRepository,
            TicketingQueryRepository ticketingQueryRepository,
            TicketTypeRepository ticketTypeRepository,
            AuditLogRepository auditLogRepository,
            TicketSalesPolicy ticketSalesPolicy
    ) {
        this.userRepository = userRepository;
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketSalesPolicy = ticketSalesPolicy;
    }

    @Transactional
    public void deactivate(String organizerEmail, UUID eventId, UUID ticketTypeId) {
        User organizer = findOrganizer(organizerEmail);
        TicketType ticketType = ticketingQueryRepository.findOrganizerTicketType(organizer.getId(), eventId, ticketTypeId)
                .orElseThrow(() -> new NotFoundException("Ticket type was not found."));

        ticketSalesPolicy.assertCanDeactivate(ticketType);
        ticketType.deactivate();
        ticketTypeRepository.save(ticketType);
        auditLogRepository.save(new AuditLog(
                organizer,
                "TicketType",
                ticketType.getId(),
                "DEACTIVATE_TICKET_TYPE",
                "Deactivated ticket type."));
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = EmailNormalizer.normalize(organizerEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
