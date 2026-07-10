package nure.com.multievent.checkout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.shared.domain.BaseEntity;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;

@Entity
@Table(name = "ticket_reservations")
public class TicketReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private final List<TicketReservationItem> items = new ArrayList<>();

    protected TicketReservation() {
    }

    public TicketReservation(User customer, Event event, Instant expiresAt) {
        this.customer = customer;
        this.event = event;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.PENDING;
    }

    public User getCustomer() {
        return customer;
    }

    public Event getEvent() {
        return event;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public List<TicketReservationItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public TicketReservationItem getPrimaryItem() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Reservation does not contain ticket items.");
        }
        return items.get(0);
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(TicketReservationItem::getQuantity).sum();
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public void addItem(TicketReservationItem item) {
        if (!items.isEmpty()) {
            throw new BusinessRuleViolationException("Only one ticket line item is supported in this checkout milestone.");
        }
        items.add(Objects.requireNonNull(item, "item must not be null"));
        item.attachTo(this);
    }

    public void changeQuantity(int quantity) {
        getPrimaryItem().changeQuantity(quantity);
    }

    public void confirm(Instant confirmedAt) {
        ensurePending();
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
    }

    public void release(Instant releasedAt) {
        ensurePending();
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = Objects.requireNonNull(releasedAt, "releasedAt must not be null");
    }

    public void expire(Instant expiredAt) {
        ensurePending();
        this.status = ReservationStatus.EXPIRED;
        this.releasedAt = Objects.requireNonNull(expiredAt, "expiredAt must not be null");
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new BusinessRuleViolationException("Reservation is no longer pending.");
        }
    }
}
