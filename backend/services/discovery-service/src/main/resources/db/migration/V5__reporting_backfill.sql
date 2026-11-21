CREATE TABLE reporting_sources (source text PRIMARY KEY, cursor uuid, synchronized_at timestamptz, completed_at timestamptz, last_error text);
INSERT INTO reporting_sources(source) VALUES ('commerce');
