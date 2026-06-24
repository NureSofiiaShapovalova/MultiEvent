package nure.com.eventflow.checkout.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class CheckoutSelectionLineForm {

    @NotNull
    private UUID ticketTypeId;

    private String ticketName;

    private String description;

    private BigDecimal price;

    private String currency;

    private int maxPerOrder;

    private int availableQuantity;

    private boolean purchasable;

    private String availabilityLabel;

    @Min(0)
    private int quantity;

    public UUID getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(UUID ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public String getTicketName() {
        return ticketName;
    }

    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getMaxPerOrder() {
        return maxPerOrder;
    }

    public void setMaxPerOrder(int maxPerOrder) {
        this.maxPerOrder = maxPerOrder;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public boolean isPurchasable() {
        return purchasable;
    }

    public void setPurchasable(boolean purchasable) {
        this.purchasable = purchasable;
    }

    public String getAvailabilityLabel() {
        return availabilityLabel;
    }

    public void setAvailabilityLabel(String availabilityLabel) {
        this.availabilityLabel = availabilityLabel;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
