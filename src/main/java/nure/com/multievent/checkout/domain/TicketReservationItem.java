package nure.com.multievent.checkout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import nure.com.multievent.shared.domain.BaseEntity;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.ticketing.domain.TicketType;

@Entity
@Table(name = "ticket_reservation_items")
public class TicketReservationItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private TicketReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(name = "ticket_name", nullable = false, length = 120)
    private String ticketName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    protected TicketReservationItem() {
    }

    public TicketReservationItem(TicketType ticketType, int quantity) {
        this.ticketType = Objects.requireNonNull(ticketType, "ticketType must not be null");
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("Quantity must be positive.");
        }
        this.ticketName = ticketType.getName();
        this.quantity = quantity;
        this.unitPrice = ticketType.getPrice();
        this.currency = ticketType.getCurrency();
    }

    public TicketReservation getReservation() {
        return reservation;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public String getTicketName() {
        return ticketName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void changeQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("Quantity must be positive.");
        }
        this.quantity = quantity;
    }

    void attachTo(TicketReservation reservation) {
        this.reservation = Objects.requireNonNull(reservation, "reservation must not be null");
    }
}
