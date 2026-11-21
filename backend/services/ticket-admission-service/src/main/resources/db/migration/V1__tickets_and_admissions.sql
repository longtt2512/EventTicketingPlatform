CREATE TABLE event_gates (
 event_id uuid PRIMARY KEY, organization_id uuid, state text NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','FROZEN')),
 snapshot jsonb NOT NULL DEFAULT '{}', frozen_at timestamptz, updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE inbox (id uuid PRIMARY KEY, received_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE TABLE issuance_obligations (
 order_id uuid PRIMARY KEY, event_id uuid NOT NULL, customer_id uuid NOT NULL, snapshot jsonb NOT NULL,
 state text NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','READY','CANCELLED')),
 expected_count integer NOT NULL CHECK(expected_count>0), attempts integer NOT NULL DEFAULT 0,
 available_at timestamptz NOT NULL DEFAULT clock_timestamp(), created_at timestamptz NOT NULL DEFAULT clock_timestamp(), completed_at timestamptz
);
CREATE INDEX issuance_pending ON issuance_obligations(available_at) WHERE state='PENDING';
CREATE TABLE tickets (
 id uuid PRIMARY KEY, order_id uuid NOT NULL REFERENCES issuance_obligations(order_id), item_id uuid NOT NULL,
 ordinal integer NOT NULL CHECK(ordinal>0), event_id uuid NOT NULL REFERENCES event_gates(event_id), customer_id uuid NOT NULL,
 ticket_type_id uuid NOT NULL, ticket_type_name text NOT NULL, buyer_name text NOT NULL, buyer_email text NOT NULL,
 status text NOT NULL DEFAULT 'ISSUED' CHECK(status IN ('ISSUED','CHECKED_IN','VOIDED')),
 credential_hash char(64) NOT NULL UNIQUE, credential_ciphertext text NOT NULL,
 version bigint NOT NULL DEFAULT 1, issued_at timestamptz NOT NULL DEFAULT clock_timestamp(), checked_in_at timestamptz,
 UNIQUE(order_id,item_id,ordinal), CHECK ((status='CHECKED_IN')=(checked_in_at IS NOT NULL))
);
CREATE INDEX ticket_owner_page ON tickets(customer_id,id);
CREATE INDEX ticket_event ON tickets(event_id,status);
CREATE TABLE admissions (
 id uuid PRIMARY KEY, ticket_id uuid NOT NULL UNIQUE REFERENCES tickets(id), event_id uuid NOT NULL REFERENCES event_gates(event_id),
 actor_id uuid NOT NULL, method text NOT NULL CHECK(method IN ('QR','MANUAL')), checked_in_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX admission_event_page ON admissions(event_id,checked_in_at DESC,id DESC);
CREATE TABLE admission_requests (
 actor_id uuid NOT NULL, request_key varchar(128) NOT NULL, event_id uuid NOT NULL,
 fingerprint char(64) NOT NULL, result jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(actor_id,request_key)
);
CREATE TABLE outbox (
 id uuid PRIMARY KEY, aggregate_id uuid NOT NULL, topic text NOT NULL, body jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), available_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 published_at timestamptz, attempts integer NOT NULL DEFAULT 0
);
CREATE INDEX outbox_pending ON outbox(available_at) WHERE published_at IS NULL;
