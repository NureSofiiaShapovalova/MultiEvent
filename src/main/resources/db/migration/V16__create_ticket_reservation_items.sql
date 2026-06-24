CREATE TABLE ticket_reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES ticket_reservations(id) ON DELETE CASCADE,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id) ON DELETE RESTRICT,
    ticket_name VARCHAR(120) NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_reservation_items_reservation_id ON ticket_reservation_items (reservation_id);
CREATE INDEX idx_ticket_reservation_items_ticket_type_id ON ticket_reservation_items (ticket_type_id);
