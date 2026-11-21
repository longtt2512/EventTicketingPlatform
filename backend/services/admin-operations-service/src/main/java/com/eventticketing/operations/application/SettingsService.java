package com.eventticketing.operations.application;

import com.eventticketing.operations.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private final JdbcTemplate db;private final ObjectMapper json;
    public SettingsService(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    public record Change(@NotNull JsonNode value,@NotBlank @Size(min=3,max=1000)String reason){}
    public List<Map<String,Object>> list(){
        return db.query("select * from admin_audit.settings order by key",(r,n)->{
            var row=new LinkedHashMap<String,Object>();row.put("key",r.getString("key"));row.put("value",read(r.getString("value")));row.put("version",r.getLong("version"));row.put("changedAt",r.getTimestamp("changed_at").toInstant());row.put("editable",true);return (Map<String,Object>)row;
        });
    }
    public Map<String,Object> view(){return Map.of("settings",list(),"fixedPolicies",Map.of("reservationMinutes",10,"checkinBeforeHours",2,"timezone","Asia/Ho_Chi_Minh"));}
    @Transactional
    public Map<String,Object> change(String key,long version,UUID actor,Change input){
        if(!Set.of("reminderLeadHours","supportEmail").contains(key))throw invalid("Key không thuộc danh sách cấu hình được phép sửa.");
        JsonNode value=input.value();
        if(key.equals("reminderLeadHours")&&(!value.isIntegralNumber()||!value.canConvertToInt()||value.intValue()<1||value.intValue()>168))throw invalid("Thời gian nhắc từ 1 đến 168 giờ, số nguyên.");
        if(key.equals("supportEmail")&&(!value.isTextual()||value.asText().length()>320||(!value.asText().isBlank()&&!value.asText().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))))throw invalid("Nhập email hỗ trợ hợp lệ hoặc để trống.");
        var before=db.queryForList("select value::text,version from admin_audit.settings where key=? for update",key).getFirst();
        if(((Number)before.get("version")).longValue()!=version)throw new ApiException(HttpStatus.CONFLICT,"STALE_VERSION","Cấu hình đã đổi. Tải lại trước khi lưu.");
        db.update("update admin_audit.settings set value=?::jsonb,version=version+1,changed_by=?,changed_at=clock_timestamp() where key=?",value.toString(),actor,key);
        var details=json.createObjectNode().put("reason",input.reason());details.set("before",read((String)before.get("value")));details.set("after",value);
        db.update("insert into admin_audit.audit_records(source,id,actor_id,resource_id,action,occurred_at,details) values ('admin-operations',?,?,?,'SETTING_CHANGED',clock_timestamp(),?::jsonb)",UUID.randomUUID(),actor,key,AuditRedaction.redact(details).toString());return view();
    }
    private JsonNode read(String value){try{return json.readTree(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private ApiException invalid(String detail){return new ApiException(HttpStatus.BAD_REQUEST,"SETTING_INVALID",detail);}
}
