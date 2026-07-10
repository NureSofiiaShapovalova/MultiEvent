package nure.com.multievent.ticketing.application;

import java.time.Instant;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.domain.TicketSalesPolicy;
import nure.com.multievent.ticketing.domain.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketingViewMapper {

    private final TicketSalesPolicy ticketSalesPolicy;

    public TicketingViewMapper(TicketSalesPolicy ticketSalesPolicy) {
        this.ticketSalesPolicy = ticketSalesPolicy;
    }

    public OrganizerTicketRowView toOrganizerRowView(TicketType ticketType, Instant now) {
        TicketInventory inventory = ticketType.getInventory();
        int availableQuantity = inventory != null ? inventory.availableQuantity() : 0;
        return new OrganizerTicketRowView(
                ticketType.getId(),
                ticketType.getEvent().getId(),
                ticketType.getEvent().getTitle(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                ticketType.getMaxPerOrder(),
                ticketType.getSalesStart(),
                ticketType.getSalesEnd(),
                ticketType.isActive(),
                inventory != null ? inventory.getTotalQuantity() : 0,
                inventory != null ? inventory.getReservedQuantity() : 0,
                inventory != null ? inventory.getSoldQuantity() : 0,
                availableQuantity,
                ticketSalesPolicy.availabilityLabel(ticketType, inventory, now));
    }

    public TicketOptionView toTicketOptionView(TicketType ticketType, Instant now) {
        TicketInventory inventory = ticketType.getInventory();
        int availableQuantity = inventory != null ? inventory.availableQuantity() : 0;
        boolean purchasable = ticketSalesPolicy.isSellable(ticketType, now) && availableQuantity > 0;
        return new TicketOptionView(
                ticketType.getId(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                ticketType.getMaxPerOrder(),
                availableQuantity,
                purchasable,
                ticketSalesPolicy.availabilityLabel(ticketType, inventory, now));
    }
}
