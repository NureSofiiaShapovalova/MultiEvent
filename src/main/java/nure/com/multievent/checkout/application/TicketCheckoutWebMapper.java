package nure.com.multievent.checkout.application;

import org.springframework.stereotype.Component;

@Component
public class TicketCheckoutWebMapper {

    public TicketCheckoutForm emptyForm() {
        TicketCheckoutForm form = new TicketCheckoutForm();
        form.setQuantity(1);
        return form;
    }

    public CreateReservationCommand toCreateCommand(java.util.UUID ticketTypeId, TicketCheckoutForm form) {
        return new CreateReservationCommand(ticketTypeId, form.getQuantity());
    }

    public AdjustReservationCommand toAdjustCommand(TicketCheckoutForm form) {
        return new AdjustReservationCommand(form.getQuantity());
    }

    public TicketCheckoutForm toForm(TicketCheckoutView view) {
        TicketCheckoutForm form = new TicketCheckoutForm();
        form.setQuantity(1);
        return form;
    }

    public TicketCheckoutForm toForm(ReservationView view) {
        TicketCheckoutForm form = new TicketCheckoutForm();
        form.setQuantity(view.quantity());
        return form;
    }
}
