-- Unassigned collections are platform cash, never invented event revenue.
CREATE TABLE global_financial_state (
 singleton integer PRIMARY KEY CHECK(singleton=1),
 ledger_id uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
 version bigint NOT NULL DEFAULT 0
);
INSERT INTO global_financial_state(singleton) VALUES(1);
