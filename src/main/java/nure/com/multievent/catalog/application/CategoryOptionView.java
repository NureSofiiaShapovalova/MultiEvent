package nure.com.multievent.catalog.application;

import java.util.UUID;

public record CategoryOptionView(
        UUID id,
        String name,
        String slug
) {
}
