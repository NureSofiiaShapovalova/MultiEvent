package nure.com.eventflow.ticketing.application;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketPricingPolicy;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.ticketing.infra.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTicketTypeService {

    private final UserRepository userRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final AuditLogRepository auditLogRepository;
    private final TicketSalesPolicy ticketSalesPolicy;
    private final TicketPricingPolicy ticketPricingPolicy;
    private final Clock clock;

    public CreateTicketTypeService(
            UserRepository userRepository,
            OrganizerEventRepository organizerEventRepository,
            TicketTypeRepository ticketTypeRepository,
            AuditLogRepository auditLogRepository,
            TicketSalesPolicy ticketSalesPolicy,
            TicketPricingPolicy ticketPricingPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketSalesPolicy = ticketSalesPolicy;
        this.ticketPricingPolicy = ticketPricingPolicy;
        this.clock = clock;
    }

    @Transactional
    public UUID create(String organizerEmail, UUID eventId, CreateTicketTypeCommand command) {
        User organizer = findOrganizer(organizerEmail);
        Event event = organizerEventRepository.findByIdAndOrganizer_Id(eventId, organizer.getId())
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        ticketSalesPolicy.assertValidEventState(event);
        validate(command);

        TicketType ticketType = new TicketType(
                event,
                command.name().trim(),
                normalizeDescription(command.description()),
                command.price(),
                command.currency().trim().toUpperCase(Locale.ROOT),
                command.maxPerOrder(),
                command.salesStart(),
                command.salesEnd(),
                true);
        TicketInventory inventory = new TicketInventory(ticketType, command.totalQuantity());
        ticketType.attachInventory(inventory);

        TicketType saved = ticketTypeRepository.save(ticketType);
        auditLogRepository.save(new AuditLog(
                organizer,
                "TicketType",
                saved.getId(),
                "CREATE_TICKET_TYPE",
                "Created ticket type at " + clock.instant()));
        return saved.getId();
    }

    private void validate(CreateTicketTypeCommand command) {
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
