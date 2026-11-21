CREATE TABLE email_deliveries (
    id UUID PRIMARY KEY,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    encrypted_body TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX email_deliveries_ready_idx ON email_deliveries(available_at) WHERE delivered_at IS NULL;
