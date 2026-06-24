package nure.com.eventflow.reporting.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrganizerRevenueRowView(
        UUID eventId,
        String eventTitle,
        long paidOrders,
        long ticketsSold,
        BigDecimal revenue,
        String currency
) {
}
