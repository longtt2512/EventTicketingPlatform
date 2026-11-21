CREATE TABLE security_audit (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    subject_user_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    reason VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX security_audit_subject_idx ON security_audit(subject_user_id, created_at);
