DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_admin_audit') THEN
  CREATE ROLE app_admin_audit LOGIN PASSWORD 'local_admin_audit';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_admin_audit;
CREATE SCHEMA IF NOT EXISTS admin_audit AUTHORIZATION app_admin_audit;
ALTER ROLE app_admin_audit SET search_path TO admin_audit,public;
