ALTER TABLE event_attendees ADD COLUMN order_id uuid;
ALTER TABLE event_attendees ADD COLUMN checked_in_at timestamptz;
