ALTER TABLE attempts ADD COLUMN outcome_at timestamptz;
-- Historical updated_at may be a later refund/reconciliation, so do not invent an outcome timestamp.
