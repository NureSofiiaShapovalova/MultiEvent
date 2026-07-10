package nure.com.multievent.ticketing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.shared.domain.BaseEntity;

@Entity
@Table(name = "ticket_types")
public class TicketType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "max_per_order", nullable = false)
    private int maxPerOrder;

    @Column(name = "sales_start", nullable = false)
    private Instant salesStart;

    @Column(name = "sales_end", nullable = false)
    private Instant salesEnd;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "ticketType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TicketInventory inventory;

    protected TicketType() {
    }

    public TicketType(
            Event event,
            String name,
            String description,
            BigDecimal price,
            String currency,
            int maxPerOrder,
            Instant salesStart,
            Instant salesEnd,
            boolean active
    ) {
        this.event = event;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.maxPerOrder = maxPerOrder;
        this.salesStart = salesStart;
        this.salesEnd = salesEnd;
        this.active = active;
    }

    public Event getEvent() {
        return event;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public int getMaxPerOrder() {
        return maxPerOrder;
    }

    public Instant getSalesStart() {
        return salesStart;
    }

    public Instant getSalesEnd() {
        return salesEnd;
    }

    public boolean isActive() {
        return active;
    }

    public TicketInventory getInventory() {
        return inventory;
    }

    public void updateDetails(
            String name,
            String description,
            BigDecimal price,
            String currency,
            int maxPerOrder,
            Instant salesStart,
            Instant salesEnd
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.maxPerOrder = maxPerOrder;
        this.salesStart = Objects.requireNonNull(salesStart, "salesStart must not be null");
        this.salesEnd = Objects.requireNonNull(salesEnd, "salesEnd must not be null");
    }

    public void attachInventory(TicketInventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory must not be null");
        inventory.attachTo(this);
    }

    public void deactivate() {
        active = false;
    }
}
