package nure.com.eventflow.organizer.domain;

import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.shared.exception.AccessDeniedDomainException;
import org.springframework.stereotype.Component;

@Component
public class EventOwnershipPolicy {

    public void assertOwnedBy(UUID organizerId, Event event) {
        if (event == null || event.getOrganizer() == null || event.getOrganizer().getId() == null) {
            throw new AccessDeniedDomainException("Event ownership could not be verified.");
        }

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new AccessDeniedDomainException("You can manage only your own events.");
        }
    }
}
