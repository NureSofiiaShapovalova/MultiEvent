CREATE TABLE provider_webhook_log (
    id UUID PRIMARY KEY,
    provider_name VARCHAR(50) NOT NULL,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    resource_id VARCHAR(120),
    verification_status VARCHAR(20) NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    raw_payload TEXT NOT NULL,
    failure_reason TEXT,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_webhook_log_provider_event UNIQUE (provider_name, event_id)
);

CREATE INDEX idx_provider_webhook_log_event_type ON provider_webhook_log (event_type);
CREATE INDEX idx_provider_webhook_log_processing_status ON provider_webhook_log (processing_status);
