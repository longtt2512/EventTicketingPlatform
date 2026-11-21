package com.eventticketing.operations.application;

import com.eventticketing.operations.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditStore {
    private final JdbcTemplate db;private final ObjectMapper json;
    public AuditStore(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @Transactional
    public void ingest(String source,JsonNode page){
        if(!page.path("data").isArray())throw new IllegalArgumentException("Audit source did not return a page");
        for(var row:page.path("data")){
            UUID id=UUID.fromString(row.required("id").asText());var safe=AuditRedaction.redact(row);
            db.update("insert into admin_audit.audit_records(source,id,actor_id,resource_id,action,occurred_at,details) values (?,?,?,?,?,?,?::jsonb) on conflict(source,id) do nothing",
                source,id,row.hasNonNull("actor_id")?UUID.fromString(row.get("actor_id").asText()):null,
                row.hasNonNull("resource_id")?row.get("resource_id").asText():null,row.required("action").asText(),Timestamp.from(Instant.parse(row.required("occurred_at").asText())),safe.toString());
        }
        db.update("insert into admin_audit.source_cursors(source,cursor,synchronized_at) values (?,?,clock_timestamp()) on conflict(source) do update set cursor=excluded.cursor,synchronized_at=excluded.synchronized_at,last_error=null",
            source,page.hasNonNull("nextCursor")?UUID.fromString(page.path("nextCursor").asText()):null);
    }
    public UUID cursor(String source){return db.queryForList("select cursor from admin_audit.source_cursors where source=?",UUID.class,source).stream().filter(Objects::nonNull).findFirst().orElse(null);}
    public void failed(String source){db.update("insert into admin_audit.source_cursors(source,last_error) values (?,'Source unavailable; retry pending') on conflict(source) do update set last_error=excluded.last_error",source);}
    public Map<String,Object> list(String action,UUID actor,String resource,Instant from,Instant to,String cursor,int limit){
        if(limit<1||limit>100||action!=null&&action.length()>100||resource!=null&&resource.length()>200||from!=null&&to!=null&&!from.isBefore(to))throw invalid();
        String query="select * from admin_audit.audit_records where 1=1";var args=new ArrayList<Object>();
        if(action!=null&&!action.isBlank()){query+=" and action=?";args.add(action);}
        if(actor!=null){query+=" and actor_id=?";args.add(actor);}
        if(resource!=null&&!resource.isBlank()){query+=" and resource_id=?";args.add(resource);}
        if(from!=null){query+=" and occurred_at>=?";args.add(Timestamp.from(from));}if(to!=null){query+=" and occurred_at<?";args.add(Timestamp.from(to));}
        if(cursor!=null&&!cursor.isBlank())try{String[] parts=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8).split("\\|",-1);if(parts.length!=3)throw new IllegalArgumentException();query+=" and (occurred_at,source,id)<(?,?,?)";args.add(Timestamp.from(Instant.parse(parts[0])));args.add(parts[1]);args.add(UUID.fromString(parts[2]));}catch(Exception ex){throw invalid();}
        args.add(limit+1);var rows=db.queryForList(query+" order by occurred_at desc,source desc,id desc limit ?",args.toArray());boolean more=rows.size()>limit;
        var result=new LinkedHashMap<String,Object>();var data=rows.stream().limit(limit).map(this::view).toList();result.put("data",data);result.put("hasMore",more);
        String next=null;if(more){var last=rows.get(limit-1);next=Base64.getUrlEncoder().withoutPadding().encodeToString((((Timestamp)last.get("occurred_at")).toInstant()+"|"+last.get("source")+"|"+last.get("id")).getBytes(StandardCharsets.UTF_8));}
        result.put("nextCursor",next);result.put("sources",db.queryForList("select source,synchronized_at,last_error from admin_audit.source_cursors order by source").stream().map(this::normalize).toList());return result;
    }
    public Map<String,Object> detail(String source,UUID id){var rows=db.queryForList("select * from admin_audit.audit_records where source=? and id=?",source,id);if(rows.isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Không tìm thấy bản ghi audit.");return view(rows.getFirst());}
    private Map<String,Object> view(Map<String,Object> row){var result=normalize(row);try{result.put("details",json.readTree(row.get("details").toString()));}catch(Exception ex){throw new IllegalStateException(ex);}return result;}
    private Map<String,Object> normalize(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k,v instanceof Timestamp t?t.toInstant():v));return result;}
    private ApiException invalid(){return new ApiException(HttpStatus.BAD_REQUEST,"AUDIT_FILTER_INVALID","Bộ lọc hoặc trang audit không hợp lệ.");}
}
