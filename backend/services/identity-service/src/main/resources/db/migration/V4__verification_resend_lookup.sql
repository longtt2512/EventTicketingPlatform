CREATE INDEX verification_resend_lookup_idx ON verification_tokens(user_id, purpose, created_at DESC);
