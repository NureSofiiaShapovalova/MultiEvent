package nure.com.eventflow.ticketing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class TicketTypeForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotBlank
    @Size(max = 3)
    private String currency = "EUR";

    @NotNull
    @Positive
    private Integer maxPerOrder;

    @NotNull
    @Positive
    private Integer totalQuantity;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime salesStart;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime salesEnd;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getMaxPerOrder() {
        return maxPerOrder;
    }

    public void setMaxPerOrder(Integer maxPerOrder) {
        this.maxPerOrder = maxPerOrder;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public LocalDateTime getSalesStart() {
        return salesStart;
    }

    public void setSalesStart(LocalDateTime salesStart) {
        this.salesStart = salesStart;
    }

    public LocalDateTime getSalesEnd() {
        return salesEnd;
    }

    public void setSalesEnd(LocalDateTime salesEnd) {
        this.salesEnd = salesEnd;
    }
}
