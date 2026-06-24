CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_moderation_actions_actor_user_id ON moderation_actions (actor_user_id);
CREATE INDEX idx_moderation_actions_event_id ON moderation_actions (event_id);
CREATE INDEX idx_moderation_actions_action ON moderation_actions (action);
