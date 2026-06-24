CREATE TABLE report_snapshots (
    id UUID PRIMARY KEY,
    report_code VARCHAR(80) NOT NULL,
    snapshot_date DATE NOT NULL,
    payload_jsonb TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_snapshots_report_code ON report_snapshots (report_code);
CREATE INDEX idx_report_snapshots_snapshot_date ON report_snapshots (snapshot_date);
