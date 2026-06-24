CREATE TABLE reservation_locks (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id) ON DELETE RESTRICT,
    reserved_quantity INT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservation_locks_order_id ON reservation_locks (order_id);
CREATE INDEX idx_reservation_locks_ticket_type_id ON reservation_locks (ticket_type_id);
CREATE INDEX idx_reservation_locks_status ON reservation_locks (status);
