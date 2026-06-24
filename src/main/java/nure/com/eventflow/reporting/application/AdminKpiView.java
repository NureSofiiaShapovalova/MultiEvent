package nure.com.eventflow.reporting.application;

import java.math.BigDecimal;
import java.util.List;

public record AdminKpiView(
        long totalUsers,
        long organizers,
        long totalEvents,
        long publishedEvents,
        long pendingOrders,
        long paidOrders,
        long failedPayments,
        BigDecimal grossRevenue,
        String currency,
        List<CategoryPopularityRowView> categoryPopularity
) {
}
