package nure.com.eventflow.checkout.application;

import java.util.UUID;

public record CreateReservationCommand(
        UUID ticketTypeId,
        int quantity
) {
}
