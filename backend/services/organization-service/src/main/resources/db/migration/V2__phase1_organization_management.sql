ALTER TABLE organizations ADD COLUMN contact_email varchar(320) NOT NULL DEFAULT '';
ALTER TABLE organizations ADD COLUMN address varchar(1000) NOT NULL DEFAULT '';
ALTER TABLE organizations ADD COLUMN description varchar(2000) NOT NULL DEFAULT '';
ALTER TABLE organizer_applications ADD COLUMN address varchar(1000) NOT NULL DEFAULT '';
ALTER TABLE organizer_applications ADD COLUMN document_id uuid;
ALTER TABLE organizer_applications ADD COLUMN organization_id uuid REFERENCES organizations(id);
ALTER TABLE organization_members ADD COLUMN display_name varchar(200) NOT NULL DEFAULT '';
ALTER TABLE organization_members ADD COLUMN email varchar(320) NOT NULL DEFAULT '';

CREATE TABLE verification_documents (
    id uuid PRIMARY KEY, owner_user_id uuid NOT NULL, object_key text NOT NULL UNIQUE,
    content_type varchar(100) NOT NULL, size_bytes bigint NOT NULL CHECK(size_bytes > 0 AND size_bytes <= 5242880),
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE organizer_applications ADD CONSTRAINT application_document_fk FOREIGN KEY (document_id) REFERENCES verification_documents(id);
CREATE TABLE organization_audit (
    id uuid PRIMARY KEY, organization_id uuid, application_id uuid, actor_user_id uuid NOT NULL,
    action varchar(100) NOT NULL, subject_id uuid, reason varchar(2000), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE payout_destinations (
    id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organizations(id),
    destination_version bigint NOT NULL CHECK(destination_version > 0),
    bank_name varchar(200) NOT NULL, account_holder varchar(200) NOT NULL,
    masked_account varchar(40) NOT NULL, encrypted_details text NOT NULL,
    status varchar(32) NOT NULL CHECK(status IN ('PENDING_REVERIFY','ACTIVE','REVOKED','REJECTED')),
    reviewed_by uuid, reviewed_at timestamptz, review_reason varchar(2000),
    created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(organization_id,destination_version)
);
CREATE UNIQUE INDEX one_active_payout ON payout_destinations(organization_id) WHERE status='ACTIVE';
CREATE TABLE event_staff_assignments (
    id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organizations(id), event_id uuid NOT NULL,
    user_id uuid NOT NULL, status varchar(20) NOT NULL CHECK(status IN ('ACTIVE','REVOKED')),
    assigned_by uuid NOT NULL, assigned_at timestamptz NOT NULL DEFAULT now(), revoked_at timestamptz,
    UNIQUE(event_id,user_id), FOREIGN KEY(organization_id,user_id) REFERENCES organization_members(organization_id,user_id)
);
CREATE INDEX assignment_user ON event_staff_assignments(user_id,event_id) WHERE status='ACTIVE';
CREATE INDEX organization_audit_history ON organization_audit(organization_id,created_at);
