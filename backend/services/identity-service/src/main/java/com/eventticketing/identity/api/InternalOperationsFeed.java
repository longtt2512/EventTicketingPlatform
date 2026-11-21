package com.eventticketing.identity.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/internal/operations")
public class InternalOperationsFeed {
 private final JdbcTemplate db;private final String token;private final ObjectMapper json;
 public InternalOperationsFeed(JdbcTemplate db,ObjectMapper json,@Value("${app.service-token}")String token){this.db=db;this.json=json;this.token=token;}
 @GetMapping("/audit") public Map<String,Object> audit(@RequestHeader(value="X-Service-Token",required=false)String supplied,@RequestParam(required=false)UUID cursor){
  guard(supplied);
  var rows=db.queryForList("select * from (select id,actor_user_id as actor_id,subject_user_id::text as resource_id,action,reason,created_at as occurred_at,null::jsonb as before_value,null::jsonb as after_value from identity.security_audit) a where (?::uuid is null or id>?) order by id limit 101",cursor,cursor);
  boolean more=rows.size()>100;var data=new ArrayList<Map<String,Object>>();
  for(var row:rows.stream().limit(100).toList()){
   var value=new LinkedHashMap<String,Object>();row.forEach((k,v)->{if(v instanceof Timestamp t)value.put(k,t.toInstant());else if(k.equals("before_value")||k.equals("after_value")){try{value.put(k,v==null?null:json.readTree(v.toString()));}catch(Exception ex){throw new IllegalStateException(ex);}}else value.put(k,v);});
   data.add(value);
  }
  var result=new LinkedHashMap<String,Object>();result.put("data",data);result.put("nextCursor",more?data.getLast().get("id"):null);return result;
 }
 @GetMapping("/stats") public Map<String,Object> stats(@RequestHeader(value="X-Service-Token",required=false)String supplied){guard(supplied);return db.queryForMap("select count(*) as users from identity.users");}
 private void guard(String value){if(value==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");}
}
