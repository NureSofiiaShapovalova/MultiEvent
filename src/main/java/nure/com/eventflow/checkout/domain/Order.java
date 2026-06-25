package nure.com.eventflow.checkout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;

@Entity(name = "CheckoutOrder")
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private final List<ReservationLock> reservationLocks = new ArrayList<>();

    protected Order() {
    }

    public Order(User customer, Event event, Instant expiresAt) {
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.event = Objects.requireNonNull(event, "event must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public User getCustomer() {
        return customer;
    }

    public Event getEvent() {
        return event;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<ReservationLock> getReservationLocks() {
        return Collections.unmodifiableList(reservationLocks);
    }

    public boolean isPendingPayment() {
        return status == OrderStatus.PENDING_PAYMENT;
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public void addItem(OrderItem item) {
        items.add(Objects.requireNonNull(item, "item must not be null"));
        item.attachTo(this);
    }

    public void addReservationLock(ReservationLock reservationLock) {
        reservationLocks.add(Objects.requireNonNull(reservationLock, "reservationLock must not be null"));
        reservationLock.attachTo(this);
    }

    public void updateTotals(BigDecimal totalAmount, String currency) {
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
    }

    public void markExpired() {
        ensurePendingPayment();
        this.status = OrderStatus.EXPIRED;
    }

    public void cancel() {
        ensurePendingPayment();
        this.status = OrderStatus.CANCELLED;
    }

    public void markPaid() {
        ensurePendingPayment();
        this.status = OrderStatus.PAID;
    }

    public void markRefundPending() {
        ensurePaid();
        this.status = OrderStatus.REFUND_PENDING;
    }

    public void markRefunded() {
        if (status != OrderStatus.REFUND_PENDING) {
            throw new BusinessRuleViolationException("Order is not waiting for a refund.");
        }
        this.status = OrderStatus.REFUNDED;
    }

    private void ensurePendingPayment() {
        if (!isPendingPayment()) {
            throw new BusinessRuleViolationException("Order is no longer pending payment.");
        }
    }

    private void ensurePaid() {
        if (!isPaid()) {
            throw new BusinessRuleViolationException("Only paid orders can enter the refund flow.");
        }
    }
}
