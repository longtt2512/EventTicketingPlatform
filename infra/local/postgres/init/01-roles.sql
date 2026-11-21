DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_identity') THEN
    CREATE ROLE app_identity LOGIN PASSWORD 'local_identity';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_organization') THEN
    CREATE ROLE app_organization LOGIN PASSWORD 'local_organization';
  END IF;
END
$$;

GRANT CONNECT ON DATABASE event_ticketing TO app_identity, app_organization;

CREATE SCHEMA IF NOT EXISTS identity AUTHORIZATION app_identity;
CREATE SCHEMA IF NOT EXISTS organization AUTHORIZATION app_organization;

ALTER ROLE app_identity SET search_path TO identity, public;
ALTER ROLE app_organization SET search_path TO organization, public;
