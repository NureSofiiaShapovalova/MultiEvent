CREATE TABLE ticket_inventory (
    id UUID PRIMARY KEY,
    ticket_type_id UUID NOT NULL UNIQUE REFERENCES ticket_types(id) ON DELETE CASCADE,
    total_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    sold_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_inventory_ticket_type_id ON ticket_inventory (ticket_type_id);
