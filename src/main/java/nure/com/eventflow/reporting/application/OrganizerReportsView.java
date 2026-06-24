package nure.com.eventflow.reporting.application;

import java.math.BigDecimal;
import java.util.List;

public record OrganizerReportsView(
        long paidOrders,
        long ticketsSold,
        BigDecimal revenue,
        String currency,
        long soldOutTicketTypes,
        List<OrganizerRevenueRowView> revenueRows
) {
}
