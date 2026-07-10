package nure.com.multievent.organizer.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateEventCommand(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 220) String slug,
        @NotBlank @Size(max = 500) String shortDescription,
        @NotBlank String fullDescription,
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 150) String venueName,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 200) String addressLine,
        @Size(max = 30) String postalCode,
        @NotBlank @Size(max = 120) String country,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        List<String> imageUrls
) {
}
