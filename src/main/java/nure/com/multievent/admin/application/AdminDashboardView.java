package nure.com.multievent.admin.application;

public record AdminDashboardView(
        long totalUsers,
        long organizers,
        long categories,
        long events,
        long draftEvents,
        long reviewEvents,
        long publishedEvents,
        long cancelledEvents,
        long failedPayments,
        long auditEntries
) {
}
