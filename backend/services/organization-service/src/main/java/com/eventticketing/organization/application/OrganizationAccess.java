package com.eventticketing.organization.application;

import com.eventticketing.organization.api.ApiException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAccess {
    private final JdbcTemplate db;
    public OrganizationAccess(JdbcTemplate db) { this.db = db; }
    public Map<String,Object> require(UUID org, UUID user, String permission, boolean mutate) {
        var rows = db.queryForList("select o.*,m.permission from organization.organizations o join organization.organization_members m on m.organization_id=o.id where o.id=? and m.user_id=? and m.membership_status='ACTIVE'", org,user);
        if (rows.isEmpty()) throw missing();
        var row=rows.getFirst();
        boolean owner=user.equals(row.get("created_by_user_id"));
        if (permission != null && !owner && !permission.equals(row.get("permission")))
            throw new ApiException(HttpStatus.FORBIDDEN,"PERMISSION_REQUIRED","This membership does not grant the required permission.");
        if (mutate && !"ACTIVE".equals(row.get("status"))) throw new ApiException(HttpStatus.CONFLICT,"ORGANIZATION_SUSPENDED","This organization is suspended.");
        return row;
    }
    public String status(UUID org) {return db.queryForList("select status from organization.organizations where id=?",String.class,org).stream().findFirst().orElseThrow(OrganizationAccess::missing);}
    public void owner(UUID org, UUID user) {
        var row=require(org,user,null,true);
        if(!user.equals(row.get("created_by_user_id"))) throw new ApiException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only the organization owner can manage members or payout destinations.");
    }
    public void lock(UUID org) {
        if(db.queryForList("select id from organization.organizations where id=? for update",org).isEmpty()) throw missing();
    }
    public void audit(UUID org,UUID application,UUID actor,String action,UUID subject,String reason) {
        db.update("insert into organization.organization_audit(id,organization_id,application_id,actor_user_id,action,subject_id,reason) values (?,?,?,?,?,?,?)",UUID.randomUUID(),org,application,actor,action,subject,reason);
    }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","The resource was not found."); }
}
