package com.eventticketing.organization.application;
import com.eventticketing.organization.api.*;
import com.eventticketing.organization.api.OrganizationContracts.*;
import com.eventticketing.organization.infrastructure.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class StaffAssignmentService {
    private final JdbcTemplate db;private final OrganizationAccess access;private final CommerceClient commerce;private final IdentityClient identity;
    public StaffAssignmentService(JdbcTemplate db,OrganizationAccess access,CommerceClient commerce,IdentityClient identity) {this.db=db;this.access=access;this.commerce=commerce;this.identity=identity;}
    @Transactional(readOnly=true)
    public List<Assignment> list(UUID org,UUID actor,boolean admin) {
        if(!admin) access.require(org,actor,"MANAGEMENT",false);
        return db.query("select * from organization.event_staff_assignments where organization_id=? order by assigned_at desc",this::map,org);
    }
    @Transactional
    public void assign(UUID org,UUID event,UUID user,UUID actor) {
        access.lock(org);access.require(org,actor,"MANAGEMENT",true);
        var target=commerce.event(event);if(target==null || !org.equals(target.organizationId())) throw OrganizationAccess.missing();
        if(db.queryForList("select user_id from organization.organization_members where organization_id=? and user_id=? and membership_status='ACTIVE' and permission='CHECKIN'",org,user).isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"STAFF_MEMBERSHIP_REQUIRED","Choose an active check-in member from this organization.");
        var state=identity.securityState(user);if(!"ACTIVE".equals(state.status()) || !state.emailVerified()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"MEMBER_NOT_ELIGIBLE","The staff account must be active and verified.");
        int changed=db.update("insert into organization.event_staff_assignments(id,organization_id,event_id,user_id,status,assigned_by) values (?,?,?,?,'ACTIVE',?) on conflict(event_id,user_id) do update set status='ACTIVE',assigned_by=excluded.assigned_by,assigned_at=now(),revoked_at=null where organization.event_staff_assignments.status='REVOKED'",UUID.randomUUID(),org,event,user,actor);
        if(changed>0) access.audit(org,null,actor,"STAFF_ASSIGNED",user,event.toString());
    }
    @Transactional
    public void revoke(UUID org,UUID event,UUID user,UUID actor) {
        access.lock(org);access.require(org,actor,"MANAGEMENT",true);
        int changed=db.update("update organization.event_staff_assignments set status='REVOKED',revoked_at=now() where organization_id=? and event_id=? and user_id=? and status='ACTIVE'",org,event,user);
        if(changed>0) access.audit(org,null,actor,"STAFF_REVOKED",user,event.toString());
    }
    @Transactional(readOnly=true)
    public List<CommerceClient.Event> mine(UUID user) {
        return db.queryForList("select a.event_id from organization.event_staff_assignments a join organization.organization_members m on m.organization_id=a.organization_id and m.user_id=a.user_id join organization.organizations o on o.id=a.organization_id where a.user_id=? and a.status='ACTIVE' and m.membership_status='ACTIVE' and m.permission='CHECKIN' and o.status='ACTIVE'",UUID.class,user).stream().map(commerce::event).toList();
    }
    public CommerceClient.Event assigned(UUID event,UUID user) {
        return mine(user).stream().filter(e->event.equals(e.id())).findFirst().orElseThrow(OrganizationAccess::missing);
    }
    private Assignment map(java.sql.ResultSet r,int n) throws java.sql.SQLException {return new Assignment(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("event_id",UUID.class),r.getObject("user_id",UUID.class),r.getString("status"));}
}
