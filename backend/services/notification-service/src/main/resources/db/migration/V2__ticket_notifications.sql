CREATE TABLE ticket_deliveries (
 order_id uuid NOT NULL, kind text NOT NULL CHECK(kind IN ('READY','CANCELLED')), customer_id uuid NOT NULL,
 event_name text NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(), delivered_at timestamptz,
 available_at timestamptz NOT NULL DEFAULT clock_timestamp(), attempts integer NOT NULL DEFAULT 0,
 PRIMARY KEY(order_id,kind)
);
