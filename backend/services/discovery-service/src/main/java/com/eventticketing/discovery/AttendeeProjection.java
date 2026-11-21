package com.eventticketing.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendeeProjection {
 private final JdbcTemplate db;private final ObjectMapper json;
 public AttendeeProjection(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 @Transactional public void apply(String body){
  try{
   var envelope=json.readTree(body);var p=envelope.required("payload");
   if(envelope.required("eventVersion").asInt()!=1||!envelope.required("producer").asText().equals("ticket-admission"))throw new IllegalArgumentException("Unsupported attendee projection");
   UUID message=UUID.fromString(envelope.required("eventId").asText()),ticket=UUID.fromString(p.required("ticketId").asText()),event=UUID.fromString(p.required("eventId").asText());
   long version=p.required("version").asLong(-1);String status=p.required("status").asText();if(version<0||!Set.of("ISSUED","CHECKED_IN","VOIDED").contains(status))throw new IllegalArgumentException("Invalid ticket state");
   if(db.update("insert into discovery.inbox(event_id) values (?) on conflict do nothing",message)==0)return;
   db.update("insert into discovery.event_attendees(ticket_id,event_id,version,attendee_name,attendee_email,ticket_type_name,status,order_id,checked_in_at) values (?,?,?,?,?,?,?,?,?) on conflict(ticket_id) do update set version=excluded.version,attendee_name=excluded.attendee_name,attendee_email=excluded.attendee_email,ticket_type_name=excluded.ticket_type_name,status=excluded.status,order_id=excluded.order_id,checked_in_at=excluded.checked_in_at,updated_at=clock_timestamp() where event_attendees.version<excluded.version and event_attendees.event_id=excluded.event_id",ticket,event,version,p.required("attendeeName").asText(),p.required("attendeeEmail").asText(),p.required("ticketTypeName").asText(),status,p.hasNonNull("orderId")?UUID.fromString(p.get("orderId").asText()):null,p.hasNonNull("checkedInAt")?java.sql.Timestamp.from(java.time.Instant.parse(p.get("checkedInAt").asText())):null);
  }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalArgumentException("Invalid attendee event",ex);}
 }
 public Map<String,Object> list(UUID event,UUID after,int limit){
  if(limit<1||limit>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var rows=db.queryForList("select ticket_id as id,attendee_name as name,attendee_email as email,ticket_type_name as \"ticketType\",status,order_id as \"orderId\",checked_in_at as \"checkedInAt\",updated_at as \"dataUpdatedAt\" from discovery.event_attendees where event_id=? and (?::uuid is null or ticket_id>?) order by ticket_id limit ?",event,after,after,limit+1);
  boolean more=rows.size()>limit;if(more)rows=rows.subList(0,limit);var result=new LinkedHashMap<String,Object>();result.put("data",rows);result.put("hasMore",more);result.put("nextCursor",more?rows.getLast().get("id"):null);return result;
 }
}
@RestController
class InternalAttendeeController {
 private final AttendeeProjection projection;private final String token;
 InternalAttendeeController(AttendeeProjection projection,@Value("${app.service-token:local-service-token}") String token){this.projection=projection;this.token=token;}
 @GetMapping("/internal/events/{event}/attendees") Map<String,Object> list(@PathVariable UUID event,@RequestHeader(value="X-Service-Token",required=false) String supplied,@RequestParam(required=false) UUID cursor,@RequestParam(defaultValue="50") int limit){
  if(supplied==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  return projection.list(event,cursor,limit);
 }
}
@Service
class AttendeeListener {
 private final AttendeeProjection projection;AttendeeListener(AttendeeProjection projection){this.projection=projection;}
 @KafkaListener(topics={"ticket.ticket.issued.v1","ticket.ticket.voided.v1","ticket.admission.checked-in.v1"},groupId="event-attendees-v1",autoStartup="${app.messaging.enabled:true}")
 public void receive(String body,Acknowledgment ack){projection.apply(body);ack.acknowledge();}
}
