package nure.com.eventflow.checkout.domain;

import java.time.Instant;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import org.springframework.stereotype.Component;

@Component
public class ReservationPolicy {

    private final TicketSalesPolicy ticketSalesPolicy;

    public ReservationPolicy(TicketSalesPolicy ticketSalesPolicy) {
        this.ticketSalesPolicy = ticketSalesPolicy;
    }

    public void assertCanReserve(TicketType ticketType, TicketInventory inventory, int quantity, Instant now) {
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

    public void assertCanConfirm(Order order, Instant now) {
        assertPendingPayment(order);
        if (!now.isBefore(order.getExpiresAt())) {
            throw new BusinessRuleViolationException("Order has expired.");
        }
    }

    public void assertCanExpire(Order order, Instant now) {
        assertPendingPayment(order);
        if (now.isBefore(order.getExpiresAt())) {
            throw new BusinessRuleViolationException("Order has not expired yet.");
        }
    }

    public void assertCanRelease(ReservationLock reservationLock) {
        if (reservationLock == null) {
            throw new BusinessRuleViolationException("Reservation lock must exist.");
        }
        if (!reservationLock.isActive()) {
            throw new BusinessRuleViolationException("Reservation lock is no longer active.");
        }
    }

    public void assertExpiryOrdering(Instant expiresAt, Instant now) {
        if (expiresAt == null || now == null) {
            throw new BusinessRuleViolationException("Expiry timestamp is required.");
        }
        if (!expiresAt.isAfter(now)) {
            throw new BusinessRuleViolationException("Expiry must be after the current time.");
        }
    }

    private void assertPendingPayment(Order order) {
        if (order == null) {
            throw new BusinessRuleViolationException("Order must exist.");
        }
        if (!order.isPendingPayment()) {
            throw new BusinessRuleViolationException("Order is no longer pending payment.");
        }
    }
}
