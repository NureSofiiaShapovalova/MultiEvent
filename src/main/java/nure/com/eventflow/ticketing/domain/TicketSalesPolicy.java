package nure.com.eventflow.ticketing.domain;

import java.time.Instant;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class TicketSalesPolicy {

    public void assertValidEventState(Event event) {
        if (event == null) {
            throw new BusinessRuleViolationException("Event must exist.");
        }
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleViolationException("Ticket types cannot be managed for cancelled or finished events.");
        }
    }

    public void assertValidSalesWindow(Instant salesStart, Instant salesEnd) {
        if (salesStart == null || salesEnd == null) {
            throw new BusinessRuleViolationException("Sales start and end are required.");
        }
        if (!salesEnd.isAfter(salesStart)) {
            throw new BusinessRuleViolationException("Sales end must be after sales start.");
        }
    }

    public void assertValidMaxPerOrder(int maxPerOrder) {
        if (maxPerOrder <= 0) {
            throw new BusinessRuleViolationException("Max per order must be positive.");
        }
    }

    public void assertValidQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("Quantity must be positive.");
        }
    }

    public void assertCanDeactivate(TicketType ticketType) {
        if (ticketType == null) {
            throw new BusinessRuleViolationException("Ticket type must exist.");
        }
        if (!ticketType.isActive()) {
            throw new BusinessRuleViolationException("Ticket type is already inactive.");
        }
    }

    public boolean isSellable(TicketType ticketType, Instant now) {
        if (ticketType == null || now == null) {
            return false;
        }
        return ticketType.isActive()
                && !now.isBefore(ticketType.getSalesStart())
                && !now.isAfter(ticketType.getSalesEnd());
    }

    public String availabilityLabel(TicketType ticketType, TicketInventory inventory, Instant now) {
        if (ticketType == null) {
            return "Unavailable";
        }
        if (!ticketType.isActive()) {
            return "Inactive";
        }
        if (now.isBefore(ticketType.getSalesStart())) {
            return "Sales not started";
        }
        if (now.isAfter(ticketType.getSalesEnd())) {
            return "Sales ended";
        }
        if (inventory != null && inventory.availableQuantity() <= 0) {
            return "Sold out";
        }
        return "On sale";
    }
}
