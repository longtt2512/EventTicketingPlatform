CREATE TABLE inbox (id uuid PRIMARY KEY,received_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE TABLE email_deliveries (
 order_id uuid PRIMARY KEY, customer_id uuid NOT NULL, event_name text NOT NULL, total_vnd bigint NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), delivered_at timestamptz,
 available_at timestamptz NOT NULL DEFAULT clock_timestamp(), attempts integer NOT NULL DEFAULT 0
);
