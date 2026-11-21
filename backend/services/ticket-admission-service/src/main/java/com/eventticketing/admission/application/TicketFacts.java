package com.eventticketing.admission.application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
public class TicketFacts {
 private final JdbcTemplate db;private final ObjectMapper json;
 public TicketFacts(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public void ticket(UUID id,String topic){var r=db.queryForMap("select * from ticket_admission.tickets where id=?",id);var p=new LinkedHashMap<String,Object>();p.put("ticketId",id);p.put("eventId",r.get("event_id"));p.put("orderId",r.get("order_id"));p.put("version",r.get("version"));p.put("attendeeName",r.get("buyer_name"));p.put("attendeeEmail",r.get("buyer_email"));p.put("ticketTypeName",r.get("ticket_type_name"));p.put("status",r.get("status"));p.put("checkedInAt",r.get("checked_in_at")==null?null:((java.sql.Timestamp)r.get("checked_in_at")).toInstant());emit(id,topic,p);}
 public void emit(UUID aggregate,String topic,Object payload){UUID id=UUID.randomUUID();try{var body=Map.of("eventId",id,"eventType",topic,"eventVersion",1,"occurredAt",Instant.now(),"producer","ticket-admission","aggregateId",aggregate,"aggregateType","ticket","correlationId",id,"causationId",id,"payload",payload);db.update("insert into ticket_admission.outbox(id,aggregate_id,topic,body) values (?,?,?,?::jsonb)",id,aggregate,topic,json.writeValueAsString(body));}catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}}
}
