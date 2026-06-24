package nure.com.eventflow.ticketing.application;

import java.util.Locale;
import java.util.UUID;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import nure.com.eventflow.ticketing.domain.TicketPricingPolicy;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketingQueryRepository;
import nure.com.eventflow.ticketing.infra.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateTicketTypeService {

    private final UserRepository userRepository;
    private final TicketingQueryRepository ticketingQueryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final AuditLogRepository auditLogRepository;
    private final TicketSalesPolicy ticketSalesPolicy;
    private final TicketPricingPolicy ticketPricingPolicy;

    public UpdateTicketTypeService(
            UserRepository userRepository,
            TicketingQueryRepository ticketingQueryRepository,
            TicketTypeRepository ticketTypeRepository,
            AuditLogRepository auditLogRepository,
            TicketSalesPolicy ticketSalesPolicy,
            TicketPricingPolicy ticketPricingPolicy
    ) {
        this.userRepository = userRepository;
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketSalesPolicy = ticketSalesPolicy;
        this.ticketPricingPolicy = ticketPricingPolicy;
    }

    @Transactional
    public void update(String organizerEmail, UUID eventId, UUID ticketTypeId, UpdateTicketTypeCommand command) {
        User organizer = findOrganizer(organizerEmail);
        TicketType ticketType = ticketingQueryRepository.findOrganizerTicketType(organizer.getId(), eventId, ticketTypeId)
                .orElseThrow(() -> new NotFoundException("Ticket type was not found."));

        ticketSalesPolicy.assertValidEventState(ticketType.getEvent());
        validate(command);

        ticketType.updateDetails(
                command.name().trim(),
                normalizeDescription(command.description()),
                command.price(),
                command.currency().trim().toUpperCase(Locale.ROOT),
                command.maxPerOrder(),
                command.salesStart(),
                command.salesEnd());
        ticketType.getInventory().changeTotalQuantity(command.totalQuantity());

        ticketTypeRepository.save(ticketType);
        auditLogRepository.save(new AuditLog(
                organizer,
                "TicketType",
                ticketType.getId(),
                "UPDATE_TICKET_TYPE",
                "Updated ticket type."));
    }

    private void validate(UpdateTicketTypeCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new BusinessRuleViolationException("Ticket type name must not be blank.");
        }
        ticketPricingPolicy.assertValidPrice(command.price());
        ticketPricingPolicy.assertValidCurrency(command.currency());
        ticketSalesPolicy.assertValidMaxPerOrder(command.maxPerOrder());
        ticketSalesPolicy.assertValidQuantity(command.totalQuantity());
        ticketSalesPolicy.assertValidSalesWindow(command.salesStart(), command.salesEnd());
    }

    private User findOrganizer(String organizerEmail) {
        String normalizedEmail = EmailNormalizer.normalize(organizerEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
