package nure.com.eventflow.checkout.application;

import java.util.List;
import java.util.UUID;

public record TicketSelectionCommand(
        List<Selection> selections
) {
    public record Selection(
            UUID ticketTypeId,
            int quantity
    ) {
    }
}
