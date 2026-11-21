CREATE TABLE configuration_gate (id integer PRIMARY KEY CHECK(id=1), version bigint NOT NULL DEFAULT 0);
INSERT INTO configuration_gate(id) VALUES (1);
CREATE TABLE commission_configs (
 id uuid PRIMARY KEY, mode text NOT NULL CHECK(mode IN ('PERCENTAGE','FIXED_AMOUNT')),
 rate_bps integer, fixed_fee_vnd bigint, effective_at timestamptz NOT NULL UNIQUE,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), created_by uuid,
 reason text NOT NULL, version bigint NOT NULL UNIQUE,
 CHECK((mode='PERCENTAGE' AND rate_bps IS NOT NULL AND rate_bps BETWEEN 0 AND 10000 AND fixed_fee_vnd IS NULL)
    OR (mode='FIXED_AMOUNT' AND rate_bps IS NULL AND fixed_fee_vnd IS NOT NULL AND fixed_fee_vnd>=0))
);
INSERT INTO commission_configs(id,mode,rate_bps,effective_at,reason,version)
VALUES ('a315b8a0-f625-40aa-bbdb-95a6092d5090','PERCENTAGE',500,clock_timestamp(),
 'Accepted local Phase 5 policy: decision 0005',0);
CREATE TABLE commission_quotes (
 order_id uuid PRIMARY KEY, request_hash text NOT NULL, snapshot jsonb NOT NULL,
 captured_at timestamptz NOT NULL
);
CREATE TABLE audit (
 id uuid PRIMARY KEY, resource_id uuid NOT NULL, actor_id uuid, action text NOT NULL,
 before_value jsonb, after_value jsonb NOT NULL, reason text NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE outbox (
 id uuid PRIMARY KEY, aggregate_id uuid NOT NULL, topic text NOT NULL, body jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), published_at timestamptz,
 attempts integer NOT NULL DEFAULT 0, available_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX outbox_pending ON outbox(available_at,created_at) WHERE published_at IS NULL;
CREATE TABLE inbox(id uuid PRIMARY KEY,received_at timestamptz NOT NULL DEFAULT clock_timestamp());
