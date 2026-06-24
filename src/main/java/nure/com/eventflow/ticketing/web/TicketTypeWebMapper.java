package nure.com.eventflow.ticketing.web;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import nure.com.eventflow.ticketing.application.CreateTicketTypeCommand;
import nure.com.eventflow.ticketing.application.OrganizerTicketRowView;
import nure.com.eventflow.ticketing.application.UpdateTicketTypeCommand;
import nure.com.eventflow.ticketing.domain.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketTypeWebMapper {

    public TicketTypeForm emptyForm() {
        return new TicketTypeForm();
    }

    public TicketTypeForm toForm(OrganizerTicketRowView rowView) {
        TicketTypeForm form = new TicketTypeForm();
        form.setName(rowView.name());
        form.setDescription(rowView.description());
        form.setPrice(rowView.price());
        form.setCurrency(rowView.currency());
        form.setMaxPerOrder(rowView.maxPerOrder());
        form.setTotalQuantity(rowView.totalQuantity());
        form.setSalesStart(LocalDateTime.ofInstant(rowView.salesStart(), ZoneOffset.UTC));
        form.setSalesEnd(LocalDateTime.ofInstant(rowView.salesEnd(), ZoneOffset.UTC));
        return form;
    }

    public TicketTypeForm toForm(TicketType ticketType) {
        TicketTypeForm form = new TicketTypeForm();
        form.setName(ticketType.getName());
        form.setDescription(ticketType.getDescription());
        form.setPrice(ticketType.getPrice());
        form.setCurrency(ticketType.getCurrency());
        form.setMaxPerOrder(ticketType.getMaxPerOrder());
        form.setSalesStart(LocalDateTime.ofInstant(ticketType.getSalesStart(), ZoneOffset.UTC));
        form.setSalesEnd(LocalDateTime.ofInstant(ticketType.getSalesEnd(), ZoneOffset.UTC));
        if (ticketType.getInventory() != null) {
            form.setTotalQuantity(ticketType.getInventory().getTotalQuantity());
        }
        return form;
    }

    public CreateTicketTypeCommand toCreateCommand(TicketTypeForm form) {
        return new CreateTicketTypeCommand(
                form.getName(),
                trimToNull(form.getDescription()),
                form.getPrice(),
                normalizeCurrency(form.getCurrency()),
                form.getMaxPerOrder(),
                form.getTotalQuantity(),
                form.getSalesStart().atZone(ZoneOffset.UTC).toInstant(),
                form.getSalesEnd().atZone(ZoneOffset.UTC).toInstant());
    }

    public UpdateTicketTypeCommand toUpdateCommand(TicketTypeForm form) {
        return new UpdateTicketTypeCommand(
                form.getName(),
                trimToNull(form.getDescription()),
                form.getPrice(),
                normalizeCurrency(form.getCurrency()),
                form.getMaxPerOrder(),
                form.getTotalQuantity(),
                form.getSalesStart().atZone(ZoneOffset.UTC).toInstant(),
                form.getSalesEnd().atZone(ZoneOffset.UTC).toInstant());
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
