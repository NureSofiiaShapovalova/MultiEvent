package nure.com.eventflow.checkout.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CheckoutWebMapper {

    public CheckoutForm toForm(List<nure.com.eventflow.ticketing.application.TicketOptionView> ticketOptions) {
        CheckoutForm form = new CheckoutForm();
        form.setSelections(ticketOptions.stream().map(this::toSelectionLineForm).toList());
        return form;
    }

    public CheckoutForm mergeForm(List<nure.com.eventflow.ticketing.application.TicketOptionView> ticketOptions, CheckoutForm submittedForm) {
        Map<UUID, Integer> quantitiesByTicketTypeId = new LinkedHashMap<>();
        if (submittedForm != null && submittedForm.getSelections() != null) {
            for (CheckoutSelectionLineForm selection : submittedForm.getSelections()) {
                quantitiesByTicketTypeId.put(selection.getTicketTypeId(), selection.getQuantity());
            }
        }

        CheckoutForm form = new CheckoutForm();
        form.setSelections(ticketOptions.stream()
                .map(option -> {
                    CheckoutSelectionLineForm line = toSelectionLineForm(option);
                    line.setQuantity(quantitiesByTicketTypeId.getOrDefault(option.ticketTypeId(), 0));
                    return line;
                })
                .toList());
        return form;
    }

    public TicketSelectionCommand toSelectionCommand(CheckoutForm form) {
        List<TicketSelectionCommand.Selection> selections = form.getSelections().stream()
                .filter(selection -> selection.getQuantity() > 0)
                .map(selection -> new TicketSelectionCommand.Selection(selection.getTicketTypeId(), selection.getQuantity()))
                .toList();
        return new TicketSelectionCommand(selections);
    }

    private CheckoutSelectionLineForm toSelectionLineForm(nure.com.eventflow.ticketing.application.TicketOptionView ticketOption) {
        CheckoutSelectionLineForm line = new CheckoutSelectionLineForm();
        line.setTicketTypeId(ticketOption.ticketTypeId());
        line.setTicketName(ticketOption.name());
        line.setDescription(ticketOption.description());
        line.setPrice(ticketOption.price());
        line.setCurrency(ticketOption.currency());
        line.setMaxPerOrder(ticketOption.maxPerOrder());
        line.setAvailableQuantity(ticketOption.availableQuantity());
        line.setPurchasable(ticketOption.purchasable());
        line.setAvailabilityLabel(ticketOption.availabilityLabel());
        line.setQuantity(0);
        return line;
    }
}
