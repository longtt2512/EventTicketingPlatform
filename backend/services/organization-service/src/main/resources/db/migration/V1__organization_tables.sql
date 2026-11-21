CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE organizer_applications (
    id UUID PRIMARY KEY,
    applicant_user_id UUID NOT NULL,
    organization_name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(320) NOT NULL,
    description VARCHAR(2000),
    verification_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION'
        CHECK (status IN ('PENDING_VERIFICATION', 'APPROVED', 'REJECTED')),
    rejection_reason VARCHAR(2000),
    decided_by_user_id UUID,
    decided_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX organizer_application_pending_idx
    ON organizer_applications (applicant_user_id)
    WHERE status = 'PENDING_VERIFICATION';

CREATE TABLE organization_members (
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    membership_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (membership_status IN ('ACTIVE', 'REVOKED')),
    permission VARCHAR(32) NOT NULL DEFAULT 'MANAGEMENT'
        CHECK (permission IN ('MANAGEMENT', 'FINANCE', 'CHECKIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (organization_id, user_id)
);

CREATE INDEX organization_member_user_idx
    ON organization_members (user_id, organization_id)
    WHERE membership_status = 'ACTIVE';
