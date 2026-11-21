ALTER TABLE events DROP CONSTRAINT events_status_check;
ALTER TABLE events ADD CONSTRAINT events_status_check CHECK(status IN ('DRAFT','PENDING_REVIEW','APPROVED','REJECTED','PUBLISHED','ONGOING','COMPLETED','CANCELLED'));
ALTER TABLE events ADD COLUMN version bigint NOT NULL DEFAULT 0,
 ADD COLUMN category varchar(40), ADD COLUMN description text NOT NULL DEFAULT '',
 ADD COLUMN venue_name varchar(200) NOT NULL DEFAULT '', ADD COLUMN venue_address varchar(500) NOT NULL DEFAULT '',
 ADD COLUMN city varchar(100) NOT NULL DEFAULT '', ADD COLUMN timezone varchar(80) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
 ADD COLUMN start_at timestamptz, ADD COLUMN end_at timestamptz, ADD COLUMN capacity integer,
 ADD COLUMN terms text NOT NULL DEFAULT '', ADD COLUMN banner_media_id uuid,
 ADD COLUMN gallery_media_ids uuid[] NOT NULL DEFAULT '{}', ADD COLUMN featured boolean NOT NULL DEFAULT false,
 ADD COLUMN published_at timestamptz, ADD COLUMN review_reason text NOT NULL DEFAULT '',
 ADD COLUMN organization_name varchar(200) NOT NULL DEFAULT '', ADD COLUMN organization_description text NOT NULL DEFAULT '',
 ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now(),
 ADD CONSTRAINT event_time CHECK(start_at IS NULL OR end_at IS NULL OR start_at < end_at),
 ADD CONSTRAINT event_capacity CHECK(capacity IS NULL OR capacity > 0);
CREATE TABLE ticket_types (
 id uuid PRIMARY KEY, event_id uuid NOT NULL REFERENCES events(id), name varchar(160) NOT NULL,
 description text NOT NULL DEFAULT '', price bigint NOT NULL CHECK(price >= 0), quota integer NOT NULL CHECK(quota > 0),
 reserved integer NOT NULL DEFAULT 0 CHECK(reserved >= 0), sold integer NOT NULL DEFAULT 0 CHECK(sold >= 0),
 sale_start timestamptz NOT NULL, sale_end timestamptz NOT NULL, max_per_order integer NOT NULL CHECK(max_per_order > 0),
 paused boolean NOT NULL DEFAULT false, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(),
 CHECK(reserved + sold <= quota), CHECK(max_per_order <= quota), CHECK(sale_start < sale_end)
);
CREATE INDEX ticket_event ON ticket_types(event_id);
CREATE TABLE event_audit (
 id uuid PRIMARY KEY, event_id uuid NOT NULL REFERENCES events(id), actor_id uuid,
 action varchar(40) NOT NULL, reason text NOT NULL DEFAULT '', version bigint NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE media_uploads (
 id uuid PRIMARY KEY, event_id uuid NOT NULL REFERENCES events(id), actor_id uuid NOT NULL,
 kind varchar(20) NOT NULL CHECK(kind IN ('BANNER','GALLERY')), content_type varchar(30) NOT NULL,
 byte_size bigint NOT NULL CHECK(byte_size > 0 AND byte_size <= 5242880), sha256 varchar(64) NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','AVAILABLE','ABANDONED')),
 object_key text NOT NULL UNIQUE, expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE event_outbox (
 id uuid PRIMARY KEY, aggregate_id uuid NOT NULL, topic varchar(100) NOT NULL, body jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), published_at timestamptz, attempts integer NOT NULL DEFAULT 0,
 available_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX event_outbox_pending ON event_outbox(available_at,created_at) WHERE published_at IS NULL;
-- Allocation foundation is internal: only a future order transaction may invoke it.
CREATE TABLE inventory_allocations (
 id uuid PRIMARY KEY, event_id uuid NOT NULL REFERENCES events(id), expires_at timestamptz NOT NULL,
 state varchar(16) NOT NULL CHECK(state IN ('ACTIVE','CONSUMED','RELEASED','EXPIRED')),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE allocation_lines (
 allocation_id uuid NOT NULL REFERENCES inventory_allocations(id), ticket_type_id uuid NOT NULL REFERENCES ticket_types(id),
 quantity integer NOT NULL CHECK(quantity > 0), price_snapshot bigint NOT NULL CHECK(price_snapshot >= 0),
 PRIMARY KEY(allocation_id,ticket_type_id)
);
CREATE TABLE allocation_ledger (
 allocation_id uuid NOT NULL REFERENCES inventory_allocations(id), transition varchar(16) NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(allocation_id,transition)
);
