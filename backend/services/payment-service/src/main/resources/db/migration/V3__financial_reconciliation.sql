CREATE TABLE gateway_reconciliations (
 transaction_id uuid PRIMARY KEY REFERENCES transactions(id), fee_vnd bigint NOT NULL CHECK(fee_vnd>=0),
 provider_reference text NOT NULL UNIQUE, reconciled_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 provenance text NOT NULL CHECK(provenance='LOCAL_PROVIDER_LEDGER')
);
CREATE TABLE financial_versions(event_id uuid PRIMARY KEY,version bigint NOT NULL DEFAULT 0);
CREATE TABLE payout_authorizations (
 settlement_id uuid PRIMARY KEY, event_id uuid NOT NULL, organization_id uuid NOT NULL,
 request_hash text NOT NULL, payment_fingerprint text NOT NULL, organization_authorization jsonb NOT NULL,
 authorized_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
