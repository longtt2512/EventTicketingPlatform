CREATE TABLE operations_deliveries (
 obligation_key text NOT NULL, user_id uuid NOT NULL, payload jsonb NOT NULL,
 attempts integer NOT NULL DEFAULT 0, available_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), delivered_at timestamptz,
 PRIMARY KEY(obligation_key,user_id)
);
CREATE INDEX operations_delivery_pending ON operations_deliveries(available_at) WHERE delivered_at IS NULL;
