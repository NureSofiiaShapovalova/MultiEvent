package nure.com.eventflow.admin.application;

import nure.com.eventflow.admin.domain.ModerationActionType;

public record ModerationCommand(
        ModerationActionType action,
        String reason
) {
}
