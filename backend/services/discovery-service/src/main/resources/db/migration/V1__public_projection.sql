CREATE TABLE projection_versions (id uuid PRIMARY KEY, version bigint NOT NULL);
CREATE TABLE inbox (event_id uuid PRIMARY KEY, processed_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE public_events (
 id uuid PRIMARY KEY, name text NOT NULL, category varchar(40) NOT NULL, city text NOT NULL,
 start_at timestamptz NOT NULL, end_at timestamptz NOT NULL, status varchar(24) NOT NULL,
 featured boolean NOT NULL, sold bigint NOT NULL, payload jsonb NOT NULL,
 version bigint NOT NULL, projected_at timestamptz NOT NULL DEFAULT now(), source_updated_at timestamptz NOT NULL
);
CREATE INDEX public_upcoming ON public_events(start_at,id);
CREATE INDEX public_popular ON public_events(sold DESC,start_at,id);
CREATE INDEX public_category ON public_events(category,start_at,id);
