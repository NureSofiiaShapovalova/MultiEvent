package nure.com.eventflow.admin.application;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogRowView(
        UUID id,
        Instant createdAt,
        String actorEmail,
        String targetType,
        UUID targetId,
        String action,
        String details
) {
}
