package com.eventticketing.organization.application;

import com.eventticketing.organization.api.*;
import com.eventticketing.organization.infrastructure.IdentityClient;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizerApplicationService {
    private final JdbcTemplate db; private final IdentityClient identity;
    private final VerificationDocumentService documents; private final OrganizationAccess access;
    public OrganizerApplicationService(JdbcTemplate db,IdentityClient identity,VerificationDocumentService documents,OrganizationAccess access) {
        this.db=db;this.identity=identity;this.documents=documents;this.access=access;
    }
    @Transactional
    public OrganizerApplicationResponse submit(UUID actor, boolean ignored, OrganizerApplicationRequest request) {
        eligible(actor); documents.requireOwned(request.documentId(),actor);
        if(!db.queryForList("select id from organization.organizer_applications where applicant_user_id=? and status in ('PENDING_VERIFICATION','REJECTED')",actor).isEmpty())
            throw new ApiException(HttpStatus.CONFLICT,"APPLICATION_EXISTS","Review or resubmit your existing application.");
        UUID id=UUID.randomUUID();
        db.update("insert into organization.organizer_applications(id,applicant_user_id,organization_name,contact_email,description,address,document_id) values (?,?,?,?,?,?,?)",
            id,actor,request.organizationName().trim(),request.contactEmail().trim().toLowerCase(Locale.ROOT),request.description(),request.address().trim(),request.documentId());
        access.audit(null,id,actor,"APPLICATION_SUBMITTED",id,null); return get(id);
    }
    @Transactional(readOnly=true)
    public OrganizerApplicationResponse mine(UUID actor) {
        return db.query("select * from organization.organizer_applications where applicant_user_id=? order by created_at desc limit 1",this::map,actor).stream().findFirst().orElseThrow(OrganizationAccess::missing);
    }
    @Transactional(readOnly=true)
    public List<OrganizerApplicationResponse> listForAdmin() {return db.query("select * from organization.organizer_applications order by created_at desc limit 200",this::map);}
    @Transactional
    public OrganizerApplicationResponse edit(UUID id,UUID actor,OrganizerApplicationRequest request) {
        var app=lock(id); if(!app.applicantUserId().equals(actor)) throw OrganizationAccess.missing();
        state(app,"REJECTED"); documents.requireOwned(request.documentId(),actor);
        db.update("update organization.organizer_applications set organization_name=?,contact_email=?,description=?,address=?,document_id=?,version=version+1,updated_at=now() where id=?",
            request.organizationName().trim(),request.contactEmail().trim().toLowerCase(Locale.ROOT),request.description(),request.address().trim(),request.documentId(),id);
        access.audit(null,id,actor,"APPLICATION_UPDATED",id,null); return get(id);
    }
    @Transactional
    public OrganizerApplicationResponse resubmit(UUID id,UUID actor) {
        var app=lock(id); if(!app.applicantUserId().equals(actor)) throw OrganizationAccess.missing();
        state(app,"REJECTED");eligible(actor);requireComplete(app);
        db.update("update organization.organizer_applications set status='PENDING_VERIFICATION',rejection_reason=null,decided_by_user_id=null,decided_at=null,version=version+1,updated_at=now() where id=?",id);
        access.audit(null,id,actor,"APPLICATION_RESUBMITTED",id,null);return get(id);
    }
    @Transactional
    public OrganizationResponse approve(UUID id,UUID actor) {
        var app=lock(id);state(app,"PENDING_VERIFICATION");eligible(app.applicantUserId());requireComplete(app);
        UUID org=UUID.randomUUID();
        db.update("insert into organization.organizations(id,name,created_by_user_id,contact_email,address,description) values (?,?,?,?,?,?)",
            org,app.organizationName(),app.applicantUserId(),app.contactEmail(),app.address(),app.description()==null?"":app.description());
        db.update("insert into organization.organization_members(organization_id,user_id,permission) values (?,?,'MANAGEMENT')",org,app.applicantUserId());
        identity.addOrganizerRole(app.applicantUserId());
        db.update("update organization.organizer_applications set status='APPROVED',organization_id=?,decided_by_user_id=?,decided_at=now(),version=version+1,updated_at=now() where id=?",org,actor,id);
        access.audit(org,id,actor,"APPLICATION_APPROVED",id,null);
        notice(app,"APPLICATION_APPROVED","");
        return new OrganizationResponse(org,app.organizationName(),"ACTIVE",app.applicantUserId());
    }
    @Transactional
    public OrganizerApplicationResponse reject(UUID id,UUID actor,RejectApplicationRequest request) {
        var app=lock(id);state(app,"PENDING_VERIFICATION");
        db.update("update organization.organizer_applications set status='REJECTED',rejection_reason=?,decided_by_user_id=?,decided_at=now(),version=version+1,updated_at=now() where id=?",request.reason().trim(),actor,id);
        access.audit(null,id,actor,"APPLICATION_REJECTED",id,request.reason().trim());notice(app,"APPLICATION_REJECTED",request.reason().trim());return get(id);
    }
    private void notice(OrganizerApplicationResponse app,String kind,String reason){
        db.update("insert into organization.notification_outbox(id,payload) values (?,jsonb_build_object('kind',?::text,'userId',?::text,'resourceId',?::text,'name',?::text,'reason',?::text))",UUID.randomUUID(),kind,app.applicantUserId().toString(),app.id().toString(),app.organizationName(),reason);
    }
    private void requireComplete(OrganizerApplicationResponse app) {
        if(app.address()==null || app.address().isBlank() || app.documentId()==null)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"APPLICATION_INCOMPLETE","The application needs an address and verification document. Reject it for correction.");
        documents.requireOwned(app.documentId(),app.applicantUserId());
    }
    private void eligible(UUID user) {
        var s=identity.securityState(user);
        if(!"ACTIVE".equals(s.status()) || !s.emailVerified()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"APPLICANT_NOT_ELIGIBLE","An active, verified account is required.");
    }
    private void state(OrganizerApplicationResponse app,String expected) {
        if(!expected.equals(app.status())) throw new ApiException(HttpStatus.CONFLICT,"INVALID_APPLICATION_STATE","Reload the application; this action is not allowed in its current state.");
    }
    private OrganizerApplicationResponse lock(UUID id) {return db.query("select * from organization.organizer_applications where id=? for update",this::map,id).stream().findFirst().orElseThrow(OrganizationAccess::missing);}
    private OrganizerApplicationResponse get(UUID id) {return db.query("select * from organization.organizer_applications where id=?",this::map,id).stream().findFirst().orElseThrow(OrganizationAccess::missing);}
    private OrganizerApplicationResponse map(java.sql.ResultSet r,int row) throws java.sql.SQLException {
        var decided=r.getTimestamp("decided_at");
        return new OrganizerApplicationResponse(r.getObject("id",UUID.class),r.getObject("applicant_user_id",UUID.class),r.getString("organization_name"),r.getString("contact_email"),r.getString("description"),r.getString("address"),r.getObject("document_id",UUID.class),r.getObject("organization_id",UUID.class),r.getString("status"),r.getString("rejection_reason"),r.getObject("decided_by_user_id",UUID.class),decided==null?null:decided.toInstant(),r.getTimestamp("created_at").toInstant(),r.getLong("version"));
    }
}
