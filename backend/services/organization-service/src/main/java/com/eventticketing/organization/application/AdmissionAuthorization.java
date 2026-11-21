package com.eventticketing.organization.application;
import com.eventticketing.organization.api.ApiException;
import com.eventticketing.organization.infrastructure.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
public class AdmissionAuthorization {
 private final JdbcTemplate db;private final CommerceClient commerce;private final IdentityClient identity;
 public AdmissionAuthorization(JdbcTemplate db,CommerceClient commerce,IdentityClient identity){this.db=db;this.commerce=commerce;this.identity=identity;}
 public void authorize(UUID event,UUID user){var account=identity.securityState(user);if(!"ACTIVE".equals(account.status())||!account.emailVerified())throw denied();var e=commerce.event(event);var rows=db.queryForList("select o.created_by_user_id,m.permission from organization.organizations o join organization.organization_members m on m.organization_id=o.id where o.id=? and o.status='ACTIVE' and m.user_id=? and m.membership_status='ACTIVE'",e.organizationId(),user);if(rows.isEmpty())throw denied();var row=rows.getFirst();if(user.equals(row.get("created_by_user_id"))||"MANAGEMENT".equals(row.get("permission")))return;if(!"CHECKIN".equals(row.get("permission"))||db.queryForObject("select count(*) from organization.event_staff_assignments where event_id=? and organization_id=? and user_id=? and status='ACTIVE'",Integer.class,event,e.organizationId(),user)!=1)throw denied();}
 public List<UUID> events(UUID user){var state=identity.securityState(user);if(!"ACTIVE".equals(state.status())||!state.emailVerified())throw denied();var result=new TreeSet<UUID>();result.addAll(db.queryForList("select a.event_id from organization.event_staff_assignments a join organization.organization_members m on m.organization_id=a.organization_id and m.user_id=a.user_id join organization.organizations o on o.id=a.organization_id where a.user_id=? and a.status='ACTIVE' and m.membership_status='ACTIVE' and m.permission='CHECKIN' and o.status='ACTIVE'",UUID.class,user));for(UUID org:db.queryForList("select o.id from organization.organizations o join organization.organization_members m on m.organization_id=o.id where o.status='ACTIVE' and m.user_id=? and m.membership_status='ACTIVE' and (m.permission='MANAGEMENT' or o.created_by_user_id=?)",UUID.class,user,user))result.addAll(commerce.events(org));return List.copyOf(result);}
 private ApiException denied(){return new ApiException(HttpStatus.FORBIDDEN,"ADMISSION_PERMISSION_REQUIRED","Bạn không có quyền check-in tại sự kiện này hoặc tổ chức đang tạm ngưng.");}
}
