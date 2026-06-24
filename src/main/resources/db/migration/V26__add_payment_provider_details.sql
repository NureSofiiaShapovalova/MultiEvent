ALTER TABLE payments
    ADD COLUMN provider_approval_url TEXT;

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    transaction_type VARCHAR(30) NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2),
    currency VARCHAR(3),
    status VARCHAR(40) NOT NULL,
    raw_payload TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions (payment_id);
CREATE INDEX idx_payment_transactions_provider_reference ON payment_transactions (provider_reference);
CREATE INDEX idx_payment_transactions_type ON payment_transactions (transaction_type);
