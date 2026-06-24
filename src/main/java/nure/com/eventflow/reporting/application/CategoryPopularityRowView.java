package nure.com.eventflow.reporting.application;

public record CategoryPopularityRowView(
        String categoryName,
        long eventCount,
        long paidOrderCount
) {
}
