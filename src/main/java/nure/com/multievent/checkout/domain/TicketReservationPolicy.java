package nure.com.multievent.checkout.domain;

import java.time.Instant;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.domain.TicketSalesPolicy;
import nure.com.multievent.ticketing.domain.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketReservationPolicy {

    private final TicketSalesPolicy ticketSalesPolicy;

    public TicketReservationPolicy(TicketSalesPolicy ticketSalesPolicy) {
        this.ticketSalesPolicy = ticketSalesPolicy;
    }

    public void assertCanCreateReservation(TicketType ticketType, TicketInventory inventory, int quantity, Instant now) {
        if (ticketType == null) {
            throw new BusinessRuleViolationException("Ticket type must exist.");
        }
        ticketSalesPolicy.assertValidQuantity(quantity);
        ticketSalesPolicy.assertValidMaxPerOrder(ticketType.getMaxPerOrder());
        if (quantity > ticketType.getMaxPerOrder()) {
            throw new BusinessRuleViolationException("Quantity cannot exceed max per order.");
        }
        if (!ticketSalesPolicy.isSellable(ticketType, now)) {
            throw new BusinessRuleViolationException("Ticket type is not available for reservation.");
        }
        if (inventory == null || inventory.availableQuantity() < quantity) {
            throw new BusinessRuleViolationException("Not enough inventory available.");
        }
    }

    public void assertCanAdjustReservation(TicketReservation reservation, TicketInventory inventory, int newQuantity, Instant now) {
        if (reservation == null) {
            throw new BusinessRuleViolationException("Reservation must exist.");
        }
        if (!reservation.isPending()) {
            throw new BusinessRuleViolationException("Reservation cannot be modified.");
        }
        if (!now.isBefore(reservation.getExpiresAt())) {
            throw new BusinessRuleViolationException("Reservation has expired.");
        }
        ticketSalesPolicy.assertValidQuantity(newQuantity);
        TicketType ticketType = reservation.getPrimaryItem().getTicketType();
        if (newQuantity > ticketType.getMaxPerOrder()) {
            throw new BusinessRuleViolationException("Quantity cannot exceed max per order.");
        }
        if (!ticketSalesPolicy.isSellable(ticketType, now)) {
            throw new BusinessRuleViolationException("Ticket type is not available for reservation.");
        }
        int currentQuantity = reservation.getTotalQuantity();
        int delta = newQuantity - currentQuantity;
        if (delta > 0 && (inventory == null || inventory.availableQuantity() < delta)) {
            throw new BusinessRuleViolationException("Not enough inventory available.");
        }
    }

    public void assertCanConfirm(TicketReservation reservation, Instant now) {
        assertReservationPending(reservation);
        if (!now.isBefore(reservation.getExpiresAt())) {
            throw new BusinessRuleViolationException("Reservation has expired.");
        }
    }

    public void assertCanRelease(TicketReservation reservation) {
        assertReservationPending(reservation);
    }

    public void assertExpiryOrdering(Instant expiresAt, Instant now) {
        if (expiresAt == null || now == null) {
            throw new BusinessRuleViolationException("Expiry timestamp is required.");
        }
        if (!expiresAt.isAfter(now)) {
            throw new BusinessRuleViolationException("Expiry must be after the current time.");
        }
    }

    private void assertReservationPending(TicketReservation reservation) {
        if (reservation == null) {
            throw new BusinessRuleViolationException("Reservation must exist.");
        }
        if (!reservation.isPending()) {
            throw new BusinessRuleViolationException("Reservation is no longer pending.");
        }
    }
}
