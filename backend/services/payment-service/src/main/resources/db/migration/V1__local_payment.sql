CREATE TABLE attempts (
 id uuid PRIMARY KEY, order_id uuid NOT NULL, customer_id uuid NOT NULL, event_id uuid NOT NULL, organization_id uuid NOT NULL,
 expected_amount bigint NOT NULL CHECK(expected_amount>0), currency varchar(3) NOT NULL DEFAULT 'VND',
 status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
 result varchar(40) NOT NULL DEFAULT 'PENDING', expires_at timestamptz NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX attempt_order ON attempts(order_id,created_at DESC,id DESC);
CREATE TABLE mock_sessions (
 attempt_id uuid PRIMARY KEY REFERENCES attempts(id), reference varchar(100) NOT NULL UNIQUE,
 status varchar(16) NOT NULL DEFAULT 'PENDING', scenario varchar(32), received_amount bigint NOT NULL,
 available_at timestamptz, delivered_at timestamptz, paid_at timestamptz, duplicate_delivery boolean NOT NULL DEFAULT false
);
CREATE TABLE receipts (
 id uuid PRIMARY KEY, payload_hash varchar(64) NOT NULL, reference varchar(100) NOT NULL,
 received_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE transactions (
 id uuid PRIMARY KEY, attempt_id uuid REFERENCES attempts(id), order_id uuid, event_id uuid,
 provider varchar(32) NOT NULL, merchant varchar(64) NOT NULL, reference varchar(100) NOT NULL,
 amount bigint NOT NULL CHECK(amount>=0), currency varchar(3) NOT NULL, status varchar(16) NOT NULL,
 result varchar(40) NOT NULL DEFAULT 'PENDING', paid_at timestamptz,
 received_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(provider,merchant,reference)
);
CREATE TABLE exceptions (
 id uuid PRIMARY KEY, transaction_id uuid NOT NULL UNIQUE REFERENCES transactions(id),
 order_id uuid, event_id uuid, type varchar(40) NOT NULL, status varchar(16) NOT NULL DEFAULT 'OPEN'
 CHECK(status IN ('OPEN','IN_REVIEW','RESOLVED')), expected_amount bigint, received_amount bigint NOT NULL,
 returned_amount bigint NOT NULL DEFAULT 0 CHECK(returned_amount>=0 AND returned_amount<=received_amount),
 currency varchar(3) NOT NULL, reviewer_id uuid, reason text NOT NULL DEFAULT '',
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE return_evidence (
 id uuid PRIMARY KEY, exception_id uuid NOT NULL REFERENCES exceptions(id), reference varchar(100) NOT NULL UNIQUE,
 amount bigint NOT NULL CHECK(amount>0), currency varchar(3) NOT NULL, evidence text NOT NULL,
 returned_at timestamptz NOT NULL, actor_id uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE audit (
 id uuid PRIMARY KEY, resource_id uuid NOT NULL, actor_id uuid, action varchar(40) NOT NULL,
 detail text NOT NULL DEFAULT '', occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE inbox (id uuid PRIMARY KEY,received_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE TABLE outbox (
 id uuid PRIMARY KEY, aggregate_id uuid NOT NULL, topic varchar(100) NOT NULL, body jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), published_at timestamptz, attempts integer NOT NULL DEFAULT 0,
 available_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX payment_outbox_pending ON outbox(available_at,created_at) WHERE published_at IS NULL;
CREATE TABLE reconciliation_runs (
 id uuid PRIMARY KEY, actor_id uuid NOT NULL, examined integer NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), completed_at timestamptz
);
