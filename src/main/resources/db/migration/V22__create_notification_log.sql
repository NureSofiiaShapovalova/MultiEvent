CREATE TABLE notification_log (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    channel VARCHAR(20) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    payload_jsonb TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_log_recipient_user_id ON notification_log (recipient_user_id);
CREATE INDEX idx_notification_log_status ON notification_log (status);
