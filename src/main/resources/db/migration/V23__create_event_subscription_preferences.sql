CREATE TABLE event_subscription_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL,
    order_updates_enabled BOOLEAN NOT NULL,
    marketing_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_subscription_preferences_user UNIQUE (user_id)
);

CREATE INDEX idx_event_subscription_preferences_user_id ON event_subscription_preferences (user_id);
