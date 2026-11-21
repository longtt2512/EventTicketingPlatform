CREATE TABLE settlements (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, event_id uuid NOT NULL UNIQUE,
 status text NOT NULL CHECK(status IN ('PENDING','PROCESSING','SETTLED','FAILED')),
 snapshot jsonb NOT NULL, version bigint NOT NULL DEFAULT 0,
 requested_by uuid NOT NULL, requested_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 decision jsonb, decision_actor uuid, authorization_proof jsonb,
 transfer_status text NOT NULL DEFAULT 'NOT_STARTED' CHECK(transfer_status IN ('NOT_STARTED','UNKNOWN','PAID','NOT_PAID','NO_TRANSFER')),
 transfer_reference text UNIQUE, completed_at timestamptz,
 CHECK(status<>'SETTLED' OR (completed_at IS NOT NULL AND transfer_status IN ('PAID','NO_TRANSFER')))
);
CREATE TABLE settlement_items (
 order_id uuid PRIMARY KEY, settlement_id uuid NOT NULL REFERENCES settlements(id),
 amount_vnd bigint NOT NULL CHECK(amount_vnd>=0)
);
CREATE TABLE settlement_requests (
 actor_id uuid NOT NULL, request_key text NOT NULL, event_id uuid NOT NULL,
 settlement_id uuid NOT NULL REFERENCES settlements(id), PRIMARY KEY(actor_id,request_key)
);
CREATE TABLE confirmed_orders (
 order_id uuid PRIMARY KEY, organization_id uuid NOT NULL, event_id uuid NOT NULL,
 payload jsonb NOT NULL, confirmed_at timestamptz NOT NULL, source_version bigint NOT NULL
);
CREATE TABLE financial_events (
 event_id uuid PRIMARY KEY, payload jsonb NOT NULL, source_version bigint NOT NULL,
 projected_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
