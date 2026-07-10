package nure.com.multievent.checkout.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TicketCheckoutForm {

    @NotNull
    @Positive
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
