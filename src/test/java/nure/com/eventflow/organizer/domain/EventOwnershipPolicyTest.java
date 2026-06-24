package nure.com.eventflow.organizer.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import nure.com.eventflow.organizer.OrganizerTestFixtures;
import nure.com.eventflow.shared.exception.AccessDeniedDomainException;
import org.junit.jupiter.api.Test;

class EventOwnershipPolicyTest {

    private final EventOwnershipPolicy eventOwnershipPolicy = new EventOwnershipPolicy();

    @Test
    void allowsOrganizerToManageOwnEvent() {
        var organizerId = UUID.randomUUID();
        var organizer = OrganizerTestFixtures.user("organizer@example.com", organizerId);
        var event = OrganizerTestFixtures.event(organizer, OrganizerTestFixtures.category(), OrganizerTestFixtures.venue(), nure.com.eventflow.catalog.domain.EventStatus.DRAFT);

        eventOwnershipPolicy.assertOwnedBy(organizerId, event);
    }

    @Test
    void rejectsDifferentOrganizer() {
        var owner = OrganizerTestFixtures.user("owner@example.com", UUID.randomUUID());
        var otherOrganizerId = UUID.randomUUID();
        var event = OrganizerTestFixtures.event(owner, OrganizerTestFixtures.category(), OrganizerTestFixtures.venue(), nure.com.eventflow.catalog.domain.EventStatus.DRAFT);

        assertThatThrownBy(() -> eventOwnershipPolicy.assertOwnedBy(otherOrganizerId, event))
                .isInstanceOf(AccessDeniedDomainException.class)
                .hasMessage("You can manage only your own events.");
    }
}
