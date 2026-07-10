package nure.com.multievent.checkout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import nure.com.multievent.shared.domain.BaseEntity;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.ticketing.domain.TicketType;

@Entity
@Table(name = "reservation_locks")
public class ReservationLock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    protected ReservationLock() {
    }

    public ReservationLock(TicketType ticketType, int reservedQuantity, Instant expiresAt) {
        this.ticketType = Objects.requireNonNull(ticketType, "ticketType must not be null");
        if (reservedQuantity <= 0) {
            throw new BusinessRuleViolationException("Reserved quantity must be positive.");
        }
        this.reservedQuantity = reservedQuantity;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = ReservationStatus.PENDING;
    }

    public Order getOrder() {
        return order;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == ReservationStatus.PENDING;
    }

    public void release(Instant releasedAt) {
        ensureActive();
        this.status = ReservationStatus.RELEASED;
        this.expiresAt = Objects.requireNonNull(releasedAt, "releasedAt must not be null");
    }

    public void confirm() {
        ensureActive();
        this.status = ReservationStatus.CONFIRMED;
    }

    public void expire(Instant expiredAt) {
        ensureActive();
        this.status = ReservationStatus.EXPIRED;
        this.expiresAt = Objects.requireNonNull(expiredAt, "expiredAt must not be null");
    }

    void attachTo(Order order) {
        this.order = Objects.requireNonNull(order, "order must not be null");
    }

    private void ensureActive() {
        if (!isActive()) {
            throw new BusinessRuleViolationException("Reservation lock is no longer active.");
        }
    }
}
