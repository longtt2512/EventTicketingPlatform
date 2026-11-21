package com.eventticketing.commerce.application;
import com.eventticketing.commerce.api.ApiException;
import com.eventticketing.commerce.infrastructure.OrganizationScopeClient;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class DraftEventService {
    private final JdbcTemplate db;private final OrganizationScopeClient scope;
    public DraftEventService(JdbcTemplate db,OrganizationScopeClient scope) {this.db=db;this.scope=scope;}
    @Transactional public Event create(UUID org,UUID user,String name) {
        scope.require(org,user,true);UUID id=UUID.randomUUID();
        db.update("insert into event_commerce.events(id,organization_id,name,created_by_user_id) values (?,?,?,?)",id,org,name.trim(),user);
        db.update("insert into event_commerce.event_audit(id,event_id,actor_id,action,version) values (?,?,?,'CREATED',0)",UUID.randomUUID(),id,user);
        return get(id);
    }
    public List<Event> list(UUID org,UUID user) {scope.require(org,user,false);return db.query("select * from event_commerce.events where organization_id=? order by created_at desc",this::map,org);}
    public Event get(UUID id) {return db.query("select * from event_commerce.events where id=?",this::map,id).stream().findFirst().orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Event was not found."));}
    private Event map(java.sql.ResultSet r,int row) throws java.sql.SQLException {return new Event(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getString("name"),r.getString("status"));}
    public record Event(UUID id,UUID organizationId,String name,String status) {}
}
