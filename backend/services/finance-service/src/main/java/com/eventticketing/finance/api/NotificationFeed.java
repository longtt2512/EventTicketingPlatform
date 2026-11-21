package com.eventticketing.finance.api;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/internal/notifications")
public class NotificationFeed {
 private final JdbcTemplate db;private final ObjectMapper json;private final String token;
 public NotificationFeed(JdbcTemplate db,ObjectMapper json,@Value("${app.identity.service-token}")String token){this.db=db;this.json=json;this.token=token;}
 @GetMapping public List<JsonNode> pending(@RequestHeader(value="X-Service-Token",required=false)String supplied){guard(supplied);return db.query("select id,payload::text from finance.notification_outbox where acknowledged_at is null order by created_at,id limit 100",(r,n)->{try{var p=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(r.getString("payload"));p.put("id",r.getString("id"));return p;}catch(Exception ex){throw new IllegalStateException(ex);}});}
 @PostMapping("/{id}/ack") public void ack(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false)String supplied){guard(supplied);db.update("update finance.notification_outbox set acknowledged_at=clock_timestamp() where id=? and acknowledged_at is null",id);}

 private void guard(String value){if(value==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");}
}
