CREATE TABLE payout_authorizations (
 settlement_id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organizations(id),
 payout_id uuid NOT NULL REFERENCES payout_destinations(id), destination_version bigint NOT NULL,
 actor_id uuid NOT NULL, request_fingerprint text NOT NULL, authorized_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
