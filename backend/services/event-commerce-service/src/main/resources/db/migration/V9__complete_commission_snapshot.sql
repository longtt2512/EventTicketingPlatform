-- FIN-R02 / local P1: a present snapshot must be complete; SQL UNKNOWN is not valid.
-- Historical NULL snapshots remain NULL and cannot be backfilled by the application.
ALTER TABLE orders DROP CONSTRAINT commission_snapshot_shape;
ALTER TABLE orders ADD CONSTRAINT commission_snapshot_shape CHECK (
 commission_snapshot IS NULL OR ((
  jsonb_typeof(commission_snapshot)='object'
  AND jsonb_typeof(commission_snapshot->'configId')='string'
  AND jsonb_typeof(commission_snapshot->'capturedAt')='string'
  AND jsonb_typeof(commission_snapshot->'grossVnd')='number'
  AND jsonb_typeof(commission_snapshot->'platformFeeVnd')='number'
  AND (commission_snapshot->>'grossVnd')::bigint=total_vnd
  AND (commission_snapshot->>'platformFeeVnd')::bigint BETWEEN 0 AND total_vnd
 ) IS TRUE)
);
