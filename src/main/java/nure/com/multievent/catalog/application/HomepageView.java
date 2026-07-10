package nure.com.multievent.catalog.application;

import java.util.List;

public record HomepageView(
        List<EventCardView> upcomingEvents,
        List<EventCardView> newestEvents
) {
}
