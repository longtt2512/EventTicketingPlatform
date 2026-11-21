DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_event_commerce') THEN
  CREATE ROLE app_event_commerce LOGIN PASSWORD 'local_event_commerce';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_event_commerce;
CREATE SCHEMA IF NOT EXISTS event_commerce AUTHORIZATION app_event_commerce;
ALTER ROLE app_event_commerce SET search_path TO event_commerce, public;
