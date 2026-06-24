package nure.com.eventflow.ticketing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;
import nure.com.eventflow.shared.domain.BaseEntity;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;

@Entity
@Table(name = "ticket_inventory")
public class TicketInventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false, unique = true)
    private TicketType ticketType;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Version
    @Column(nullable = false)
    private Long version;

    protected TicketInventory() {
    }

    public TicketInventory(TicketType ticketType, int totalQuantity) {
        this.ticketType = ticketType;
        ensurePositive(totalQuantity, "totalQuantity");
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.soldQuantity = 0;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getSoldQuantity() {
        return soldQuantity;
    }

    public Long getVersion() {
        return version;
    }

    public int availableQuantity() {
        return totalQuantity - reservedQuantity - soldQuantity;
    }

    public void changeTotalQuantity(int totalQuantity) {
        ensureNonNegative(totalQuantity, "totalQuantity");
        if (totalQuantity < reservedQuantity + soldQuantity) {
            throw new BusinessRuleViolationException("Total quantity cannot be lower than reserved and sold inventory.");
        }
        this.totalQuantity = totalQuantity;
    }

    public void reserve(int quantity) {
        ensurePositive(quantity, "quantity");
        if (quantity > availableQuantity()) {
            throw new BusinessRuleViolationException("Not enough inventory available.");
        }
        reservedQuantity += quantity;
    }

    public void release(int quantity) {
        ensurePositive(quantity, "quantity");
        if (quantity > reservedQuantity) {
            throw new BusinessRuleViolationException("Cannot release more inventory than reserved.");
        }
        reservedQuantity -= quantity;
    }

    public void markSold(int quantity) {
        ensurePositive(quantity, "quantity");
        if (quantity > reservedQuantity) {
            throw new BusinessRuleViolationException("Cannot sell more inventory than reserved.");
        }
        reservedQuantity -= quantity;
        soldQuantity += quantity;
    }

    void attachTo(TicketType ticketType) {
        this.ticketType = Objects.requireNonNull(ticketType, "ticketType must not be null");
    }

    private static void ensurePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new BusinessRuleViolationException(fieldName + " must be positive.");
        }
    }

    private static void ensureNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new BusinessRuleViolationException(fieldName + " must not be negative.");
        }
    }
}
