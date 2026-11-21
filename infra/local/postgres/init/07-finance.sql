DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_finance') THEN
  CREATE ROLE app_finance LOGIN PASSWORD 'local_finance';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_finance;
CREATE SCHEMA IF NOT EXISTS finance AUTHORIZATION app_finance;
ALTER ROLE app_finance SET search_path TO finance,public;
