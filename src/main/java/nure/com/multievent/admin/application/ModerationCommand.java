package nure.com.multievent.admin.application;

import nure.com.multievent.admin.domain.ModerationActionType;

public record ModerationCommand(
        ModerationActionType action,
        String reason
) {
}
