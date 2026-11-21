ALTER TABLE orders ADD COLUMN commission_snapshot jsonb;
ALTER TABLE orders ADD CONSTRAINT commission_snapshot_shape CHECK (
 commission_snapshot IS NULL OR (
  jsonb_typeof(commission_snapshot)='object' AND commission_snapshot ? 'configId'
  AND commission_snapshot ? 'capturedAt' AND commission_snapshot ? 'platformFeeVnd'
  AND (commission_snapshot->>'grossVnd')::bigint=total_vnd
  AND (commission_snapshot->>'platformFeeVnd')::bigint BETWEEN 0 AND total_vnd
 )
);
CREATE FUNCTION protect_commission_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.commission_snapshot IS DISTINCT FROM OLD.commission_snapshot THEN
  RAISE EXCEPTION 'FIN-R02: order commission snapshot is immutable; create a new order for new prices/fees';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER order_commission_immutable BEFORE UPDATE OF commission_snapshot ON orders
FOR EACH ROW EXECUTE FUNCTION protect_commission_snapshot();
