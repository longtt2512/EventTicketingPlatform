DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_ticket_admission') THEN
  CREATE ROLE app_ticket_admission LOGIN PASSWORD 'local_ticket_admission';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_ticket_admission;
CREATE SCHEMA IF NOT EXISTS ticket_admission AUTHORIZATION app_ticket_admission;
ALTER ROLE app_ticket_admission SET search_path TO ticket_admission, public;
