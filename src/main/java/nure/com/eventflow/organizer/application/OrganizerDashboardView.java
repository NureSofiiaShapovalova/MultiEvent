package nure.com.eventflow.organizer.application;

import java.util.List;

public record OrganizerDashboardView(
        String organizerEmail,
        long totalEvents,
        long draftEvents,
        long publishedEvents,
        long cancelledEvents,
        List<OrganizerEventRowView> recentEvents
) {
}
