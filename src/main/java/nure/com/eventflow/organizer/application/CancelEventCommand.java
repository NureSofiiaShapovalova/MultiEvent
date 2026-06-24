package nure.com.eventflow.organizer.application;

import jakarta.validation.constraints.Size;

public record CancelEventCommand(
        @Size(max = 1000) String notes
) {
}
