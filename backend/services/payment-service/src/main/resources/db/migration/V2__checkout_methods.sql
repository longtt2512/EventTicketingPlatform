CREATE TABLE mock_config (
 key varchar(64) PRIMARY KEY,
 value jsonb NOT NULL CHECK (jsonb_typeof(value) IN ('object','string')),
 description text NOT NULL,
 updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
INSERT INTO mock_config(key,value,description) VALUES
 ('QR_PAYMENT','{"status":"SUCCESS","delaySeconds":4,"lifetimeSeconds":600}',
  'QR outcome for new sessions. String success/failed or JSON status, delaySeconds, lifetimeSeconds, failure.code/message.'),
 ('CARD_PAYMENT','{"status":"SUCCESS","delaySeconds":4,"lifetimeSeconds":600,"cards":[{"number":"4242424242424242","status":"SUCCESS"},{"number":"5555555555554444","status":"SUCCESS"},{"number":"4000000000000002","status":"FAILED","failure":{"code":"INSUFFICIENT_FUNDS","message":"Số dư thẻ không đủ. Vui lòng chọn thẻ khác hoặc thanh toán qua QR."}}]}',
  'Card outcome and synthetic allowlist. Full submitted card numbers are not persisted.');

ALTER TABLE mock_sessions
 ADD COLUMN payment_method varchar(8) CHECK (payment_method IN ('QR','CARD')),
 ADD COLUMN method_expires_at timestamptz,
 ADD COLUMN qr_token uuid UNIQUE,
 ADD COLUMN card_last_four varchar(4),
 ADD COLUMN outcome_config jsonb,
 ADD COLUMN failure_code varchar(80),
 ADD COLUMN failure_message varchar(500);
