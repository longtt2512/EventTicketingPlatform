DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_discovery') THEN
  CREATE ROLE app_discovery LOGIN PASSWORD 'local_discovery';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_discovery;
CREATE SCHEMA IF NOT EXISTS discovery AUTHORIZATION app_discovery;
ALTER ROLE app_discovery SET search_path TO discovery, public;
