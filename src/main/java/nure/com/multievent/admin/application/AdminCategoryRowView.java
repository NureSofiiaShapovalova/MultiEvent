package nure.com.multievent.admin.application;

import java.util.UUID;

public record AdminCategoryRowView(
        UUID id,
        String name,
        String slug,
        long eventCount
) {
}
