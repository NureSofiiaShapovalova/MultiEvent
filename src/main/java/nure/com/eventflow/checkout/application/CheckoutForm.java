package nure.com.eventflow.checkout.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.List;

public class CheckoutForm {

    @Valid
    private List<CheckoutSelectionLineForm> selections = new ArrayList<>();

    public List<CheckoutSelectionLineForm> getSelections() {
        return selections;
    }

    public void setSelections(List<CheckoutSelectionLineForm> selections) {
        this.selections = selections;
    }

    @AssertTrue(message = "Select at least one ticket.")
    public boolean isSelectionChosen() {
        return selections != null && selections.stream().anyMatch(selection -> selection.getQuantity() > 0);
    }
}
