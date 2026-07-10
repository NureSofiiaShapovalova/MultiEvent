package nure.com.multievent.checkout.application;

import java.util.UUID;

public record CreateReservationCommand(
        UUID ticketTypeId,
        int quantity
) {
}
