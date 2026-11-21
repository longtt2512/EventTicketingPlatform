package com.eventticketing.finance.application;

import com.eventticketing.finance.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementRepository {
    private final JdbcTemplate db;private final ObjectMapper json;
    public SettlementRepository(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    public JdbcTemplate db(){return db;}
    public ObjectNode get(UUID id){
        var rows=db.queryForList("select * from finance.settlements where id=?",id);if(rows.isEmpty())throw missing();
        var row=rows.getFirst();var result=json.createObjectNode();
        row.forEach((key,value)->{
            String name=switch(key){case "organization_id"->"organizationId";case "event_id"->"eventId";case "requested_by"->"requestedBy";case "requested_at"->"requestedAt";case "transfer_status"->"transferStatus";case "transfer_reference"->"transferReference";case "completed_at"->"completedAt";case "decision_actor"->"decisionActor";default->key;};
            try{if(Set.of("snapshot","decision","authorization_proof").contains(key))result.set(name,value==null?json.nullNode():json.readTree(value.toString()));
            else result.set(name,json.valueToTree(value instanceof Timestamp t?t.toInstant():value));}catch(Exception ex){throw new IllegalStateException(ex);}
        });return result;
    }
    public ObjectNode lock(UUID id){if(db.queryForList("select id from finance.settlements where id=? for update",id).isEmpty())throw missing();return get(id);}
    public Map<String,Object> list(UUID org,String status,UUID cursor,int limit){
        if(limit<1||limit>100)throw CommissionService.invalid("Giới hạn trang từ 1 đến 100.");
        String sql="select id from finance.settlements where 1=1";var args=new ArrayList<Object>();
        if(org!=null){sql+=" and organization_id=?";args.add(org);}
        if(status!=null&&!status.isBlank()){if(!Set.of("PENDING","PROCESSING","SETTLED","FAILED").contains(status))throw CommissionService.invalid("Trạng thái đối soát không hợp lệ.");sql+=" and status=?";args.add(status);}
        if(cursor!=null){sql+=" and id>?";args.add(cursor);}args.add(limit+1);
        var ids=db.queryForList(sql+" order by id limit ?",UUID.class,args.toArray());boolean more=ids.size()>limit;
        var result=new LinkedHashMap<String,Object>();result.put("data",ids.stream().limit(limit).map(this::get).toList());result.put("hasMore",more);result.put("nextCursor",more?ids.get(limit-1):null);return result;
    }
    public List<Map<String,Object>> history(UUID id){
        return db.queryForList("select id,actor_id,action,reason,occurred_at from finance.audit where resource_id=? order by occurred_at,id",id).stream().map(row->{var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k,v instanceof Timestamp t?t.toInstant():v));return (Map<String,Object>)result;}).toList();
    }
    public static ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Không tìm thấy đối soát trong phạm vi của bạn.");}
}
