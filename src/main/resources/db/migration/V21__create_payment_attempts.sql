CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    attempt_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(100) NOT NULL,
    requested_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_attempts_payment_number UNIQUE (payment_id, attempt_number)
);

CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts (payment_id);
CREATE INDEX idx_payment_attempts_status ON payment_attempts (status);
