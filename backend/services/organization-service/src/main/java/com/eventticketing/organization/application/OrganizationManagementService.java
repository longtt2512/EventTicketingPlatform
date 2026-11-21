package com.eventticketing.organization.application;

import com.eventticketing.organization.api.*;
import com.eventticketing.organization.api.OrganizationContracts.*;
import com.eventticketing.organization.infrastructure.IdentityClient;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

@Service
public class OrganizationManagementService {
    private final JdbcTemplate db;private final OrganizationAccess access;private final IdentityClient identity;
    public OrganizationManagementService(JdbcTemplate db,OrganizationAccess access,IdentityClient identity) {this.db=db;this.access=access;this.identity=identity;}
    @Transactional(readOnly=true)
    public List<Detail> list(UUID actor,boolean admin) {
        var ids=admin?db.queryForList("select id from organization.organizations order by created_at desc limit 200",UUID.class)
            :db.queryForList("select organization_id from organization.organization_members where user_id=? and membership_status='ACTIVE' order by created_at",UUID.class,actor);
        return ids.stream().map(id->detail(id,actor,admin)).toList();
    }
    @Transactional(readOnly=true)
    public Detail detail(UUID id,UUID actor,boolean admin) {
        if(!admin) access.require(id,actor,null,false);
        return db.query("select * from organization.organizations where id=?",(rs,n)->{
            boolean owner=actor.equals(rs.getObject("created_by_user_id",UUID.class));
            String permission=admin?"ADMIN":db.queryForObject("select permission from organization.organization_members where organization_id=? and user_id=?",String.class,id,actor);
            return new Detail(id,rs.getString("name"),rs.getString("status"),rs.getObject("created_by_user_id",UUID.class),rs.getString("contact_email"),rs.getString("address"),rs.getString("description"),rs.getLong("version"),permission,owner);
        },id).stream().findFirst().orElseThrow(OrganizationAccess::missing);
    }
    @Transactional
    public Detail update(UUID id,UUID actor,Update request) {
        access.lock(id);access.require(id,actor,"MANAGEMENT",true);
        int changed=db.update("update organization.organizations set name=?,contact_email=?,address=?,description=?,version=version+1,updated_at=now() where id=? and version=?",
            request.name().trim(),request.contactEmail().trim().toLowerCase(Locale.ROOT),request.address().trim(),request.description()==null?"":request.description(),id,request.version());
        if(changed!=1) throw new ApiException(HttpStatus.CONFLICT,"VERSION_CONFLICT","The organization changed. Reload before saving.");
        access.audit(id,null,actor,"ORGANIZATION_UPDATED",id,null);return detail(id,actor,false);
    }
    @Transactional(readOnly=true)
    public List<Member> members(UUID id,UUID actor,boolean admin) {
        if(!admin) access.require(id,actor,"MANAGEMENT",false);
        var owner=db.queryForList("select created_by_user_id from organization.organizations where id=?",UUID.class,id).stream().findFirst().orElseThrow(OrganizationAccess::missing);
        return db.query("select * from organization.organization_members where organization_id=? order by created_at",(rs,n)->new Member(rs.getObject("user_id",UUID.class),rs.getString("email"),rs.getString("display_name"),rs.getString("permission"),rs.getString("membership_status"),owner.equals(rs.getObject("user_id",UUID.class))),id);
    }
    @Transactional
    public List<Member> add(UUID id,UUID actor,AddMember request) {
        access.lock(id);access.owner(id,actor);
        var target=identity.lookup(request.email());if(target==null) throw OrganizationAccess.missing();
        UUID owner=db.queryForObject("select created_by_user_id from organization.organizations where id=?",UUID.class,id);
        if(target.userId().equals(owner)) throw new ApiException(HttpStatus.CONFLICT,"OWNER_PROTECTED","The owner membership cannot be changed.");
        if(!db.queryForList("select user_id from organization.organization_members where organization_id=? and user_id=? and membership_status='ACTIVE'",id,target.userId()).isEmpty())
            throw new ApiException(HttpStatus.CONFLICT,"MEMBER_EXISTS","This user is already an active member. Revoke their membership before changing permissions.");
        identity.addRole(target.userId(),"CHECKIN".equals(request.permission())?"CHECKIN_STAFF":"ORGANIZER");
        db.update("insert into organization.organization_members(organization_id,user_id,permission,email,display_name) values (?,?,?,?,?) on conflict(organization_id,user_id) do update set membership_status='ACTIVE',permission=excluded.permission,email=excluded.email,display_name=excluded.display_name,updated_at=now()",id,target.userId(),request.permission(),target.email(),target.displayName());
        access.audit(id,null,actor,"MEMBER_ADDED",target.userId(),request.permission());return members(id,actor,false);
    }
    @Transactional
    public void revoke(UUID id,UUID actor,UUID target) {
        access.lock(id);access.owner(id,actor);
        if(target.equals(db.queryForObject("select created_by_user_id from organization.organizations where id=?",UUID.class,id))) throw new ApiException(HttpStatus.CONFLICT,"OWNER_PROTECTED","The owner membership cannot be revoked.");
        if(db.update("update organization.organization_members set membership_status='REVOKED',updated_at=now() where organization_id=? and user_id=? and membership_status='ACTIVE'",id,target)==0) throw OrganizationAccess.missing();
        db.update("update organization.event_staff_assignments set status='REVOKED',revoked_at=now() where organization_id=? and user_id=? and status='ACTIVE'",id,target);
        access.audit(id,null,actor,"MEMBER_REVOKED",target,null);
    }
    @Transactional
    public Detail status(UUID id,UUID admin,boolean suspend,String reason) {
        access.lock(id);
        String expected=suspend?"ACTIVE":"SUSPENDED";
        if(db.update("update organization.organizations set status=?,version=version+1,updated_at=now() where id=? and status=?",suspend?"SUSPENDED":"ACTIVE",id,expected)!=1)
            throw new ApiException(HttpStatus.CONFLICT,"INVALID_ORGANIZATION_STATE","Reload the organization before changing its status.");
        access.audit(id,null,admin,suspend?"ORGANIZATION_SUSPENDED":"ORGANIZATION_RESTORED",id,reason);return detail(id,admin,true);
    }
    @Transactional(readOnly=true)
    public List<Audit> audit(UUID id,UUID actor,boolean admin) {
        if(!admin) access.require(id,actor,"MANAGEMENT",false);
        return db.query("select * from organization.organization_audit where organization_id=? order by created_at desc limit 200",(rs,n)->new Audit(rs.getObject("id",UUID.class),rs.getString("action"),rs.getObject("actor_user_id",UUID.class),rs.getObject("subject_id",UUID.class),rs.getString("reason"),rs.getTimestamp("created_at").toInstant()),id);
    }
}
