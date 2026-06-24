package nure.com.eventflow.admin.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nure.com.eventflow.admin.domain.ModerationActionType;

public class ModerationForm {

    @NotNull
    private ModerationActionType action;

    @Size(max = 1000)
    private String reason;

    public ModerationActionType getAction() {
        return action;
    }

    public void setAction(ModerationActionType action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
