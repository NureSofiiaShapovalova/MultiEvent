package nure.com.multievent.organizer.application;

import jakarta.validation.constraints.Size;

public record CancelEventCommand(
        @Size(max = 1000) String notes
) {
}
