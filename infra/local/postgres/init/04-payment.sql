DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_payment') THEN
  CREATE ROLE app_payment LOGIN PASSWORD 'local_payment';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_payment;
CREATE SCHEMA IF NOT EXISTS payment AUTHORIZATION app_payment;
ALTER ROLE app_payment SET search_path TO payment, public;
