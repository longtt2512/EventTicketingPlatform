package com.eventticketing.admission.application;
import static com.eventticketing.admission.application.TicketModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class EventGates {
 private final JdbcTemplate db;private final ObjectMapper json;private final TicketFacts facts;
 public EventGates(JdbcTemplate db,ObjectMapper json,TicketFacts facts){this.db=db;this.json=json;this.facts=facts;}
 public void lock(UUID event){db.update("insert into ticket_admission.event_gates(event_id) values (?) on conflict do nothing",event);db.queryForList("select event_id from ticket_admission.event_gates where event_id=? for update",event);}
 public void snapshot(Event event){lock(event.id());try{db.update("update ticket_admission.event_gates set organization_id=?,snapshot=?::jsonb,updated_at=clock_timestamp() where event_id=?",event.organizationId(),json.writeValueAsString(event),event.id());}catch(Exception ex){throw new IllegalStateException(ex);}if(event.status().equals("CANCELLED")||event.cancellationPending())freeze(event.id());}
 public Event event(UUID id){try{return json.readValue(db.queryForObject("select snapshot::text from ticket_admission.event_gates where event_id=?",String.class,id),Event.class);}catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}}
 public boolean frozen(UUID id){return "FROZEN".equals(db.queryForObject("select state from ticket_admission.event_gates where event_id=?",String.class,id));}
 @Transactional public void freeze(UUID event){lock(event);if(frozen(event))return;db.update("update ticket_admission.event_gates set state='FROZEN',frozen_at=coalesce(frozen_at,clock_timestamp()),updated_at=clock_timestamp() where event_id=?",event);
  var voided=db.queryForList("update ticket_admission.tickets set status='VOIDED',version=version+1 where event_id=? and status='ISSUED' returning id",UUID.class,event);
  voided.forEach(id->facts.ticket(id,"ticket.ticket.voided.v1"));
  db.update("update ticket_admission.issuance_obligations set state='CANCELLED',completed_at=clock_timestamp() where event_id=? and state='PENDING'",event);
  for(var r:db.queryForList("select order_id,customer_id from ticket_admission.issuance_obligations where event_id=?",event))facts.emit((UUID)r.get("order_id"),"ticket.order.cancelled.v1",Map.of("orderId",r.get("order_id"),"customerId",r.get("customer_id"),"eventId",event));
 }
}
