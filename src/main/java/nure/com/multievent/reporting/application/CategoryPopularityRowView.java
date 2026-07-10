package nure.com.multievent.reporting.application;

public record CategoryPopularityRowView(
        String categoryName,
        long eventCount,
        long paidOrderCount
) {
}
