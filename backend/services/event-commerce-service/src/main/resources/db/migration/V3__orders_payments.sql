CREATE TABLE orders (
 id uuid PRIMARY KEY, customer_id uuid NOT NULL, event_id uuid NOT NULL REFERENCES events(id),
 organization_id uuid NOT NULL, event_name text NOT NULL, terms_snapshot text NOT NULL,
 status varchar(24) NOT NULL CHECK(status IN ('PENDING_PAYMENT','CONFIRMED','CANCELLED','EXPIRED')),
 total_vnd bigint NOT NULL CHECK(total_vnd>=0), currency varchar(3) NOT NULL DEFAULT 'VND',
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 updated_at timestamptz NOT NULL DEFAULT clock_timestamp(), confirmed_at timestamptz,
 payment_status varchar(20) NOT NULL DEFAULT 'NONE', active_attempt_id uuid, applied_payment_id uuid,
 version bigint NOT NULL DEFAULT 0, UNIQUE(applied_payment_id)
);
CREATE INDEX order_customer ON orders(customer_id,created_at DESC,id DESC);
CREATE INDEX order_event ON orders(event_id,created_at DESC,id DESC);
CREATE INDEX order_expiry ON orders(expires_at) WHERE status='PENDING_PAYMENT';
CREATE TABLE order_items (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES orders(id), ticket_type_id uuid NOT NULL,
 name text NOT NULL, quantity integer NOT NULL CHECK(quantity>0), unit_price bigint NOT NULL CHECK(unit_price>=0),
 UNIQUE(order_id,ticket_type_id)
);
CREATE TABLE order_requests (
 actor_id uuid NOT NULL, request_key varchar(128) NOT NULL, request_hash text NOT NULL,
 order_id uuid NOT NULL REFERENCES orders(id), PRIMARY KEY(actor_id,request_key)
);
CREATE TABLE order_payment_claims (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES orders(id), request_key varchar(128) NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'PENDING', created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(order_id,request_key)
);
CREATE TABLE order_payment_effects (
 payment_id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES orders(id), result varchar(40) NOT NULL
);
CREATE TABLE order_inbox (id uuid PRIMARY KEY, received_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE TABLE order_audit (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES orders(id), actor_id uuid,
 action varchar(40) NOT NULL, occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
