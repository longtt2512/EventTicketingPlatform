CREATE TABLE events (
 id uuid PRIMARY KEY, organization_id uuid NOT NULL, name varchar(200) NOT NULL,
 status varchar(24) NOT NULL DEFAULT 'DRAFT' CHECK(status='DRAFT'),
 created_by_user_id uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX events_organization ON events(organization_id,created_at);
