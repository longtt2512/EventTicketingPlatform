CREATE TABLE categories (
 id uuid PRIMARY KEY, name varchar(40) NOT NULL UNIQUE,
 active boolean NOT NULL DEFAULT true, version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE UNIQUE INDEX category_name_case ON categories(lower(name));
INSERT INTO categories(id,name) SELECT md5(name)::uuid,name FROM unnest(ARRAY[
 'Music & Concert','Sports','Conference','Workshop','Exhibition','Festival','Entertainment','Others'
]) AS name;
CREATE TABLE category_audit (
 id uuid PRIMARY KEY,category_id uuid NOT NULL REFERENCES categories(id),actor_id uuid NOT NULL,
 action text NOT NULL,reason text NOT NULL,occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
