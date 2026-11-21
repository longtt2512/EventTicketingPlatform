-- Read model only. Ticket & Admission remains the authority for issued ticket units.
CREATE TABLE event_attendees (
 ticket_id uuid PRIMARY KEY, event_id uuid NOT NULL, version bigint NOT NULL,
 attendee_name varchar(200) NOT NULL, attendee_email varchar(320) NOT NULL,
 ticket_type_name varchar(160) NOT NULL, status varchar(24) NOT NULL,
 updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX attendee_event ON event_attendees(event_id,ticket_id);
