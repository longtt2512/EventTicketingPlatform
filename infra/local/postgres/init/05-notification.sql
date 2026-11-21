DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='app_notification') THEN
  CREATE ROLE app_notification LOGIN PASSWORD 'local_notification';
 END IF;
END $$;
GRANT CONNECT ON DATABASE event_ticketing TO app_notification;
CREATE SCHEMA IF NOT EXISTS notification AUTHORIZATION app_notification;
ALTER ROLE app_notification SET search_path TO notification, public;
