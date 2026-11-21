package com.eventticketing.admission.application;
import static com.eventticketing.admission.application.TicketModels.*;
import com.eventticketing.admission.infrastructure.SourceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class IssuanceService {
 private final JdbcTemplate db;private final ObjectMapper json;private final SourceClient source;private final EventGates gates;private final Credentials credentials;private final TicketFacts facts;
 public IssuanceService(JdbcTemplate db,ObjectMapper json,SourceClient source,EventGates gates,Credentials credentials,TicketFacts facts){this.db=db;this.json=json;this.source=source;this.gates=gates;this.credentials=credentials;this.facts=facts;}
 @Transactional public void accept(String body){try{var e=json.readTree(body);if(e.path("eventVersion").asInt()!=1||!e.path("producer").asText().equals("event-commerce")||!e.path("eventType").asText().equals("order.confirmed"))throw new IllegalArgumentException("ETK-R01: confirmed Commerce fact required");var order=json.treeToValue(e.required("payload"),Order.class);validate(order);UUID message=UUID.fromString(e.required("eventId").asText());if(db.update("insert into ticket_admission.inbox(id) values (?) on conflict do nothing",message)==0)return;
  var prior=db.queryForList("select snapshot::text from ticket_admission.issuance_obligations where order_id=?",String.class,order.id());if(!prior.isEmpty()&&!json.readValue(prior.getFirst(),Order.class).equals(order))throw new IllegalArgumentException("ETK-R02: immutable issuance units changed");
  db.update("insert into ticket_admission.issuance_obligations(order_id,event_id,customer_id,snapshot,expected_count) values (?,?,?,?::jsonb,?) on conflict do nothing",order.id(),order.eventId(),order.customerId(),json.writeValueAsString(order),order.items().stream().mapToInt(Item::quantity).sum());
 }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalArgumentException("Invalid confirmed order contract",ex);}}
 private void validate(Order o){if(o.id()==null||o.customerId()==null||o.organizationId()==null||o.eventId()==null||!"CONFIRMED".equals(o.status())||o.items()==null||o.items().isEmpty()||o.items().size()>100)throw new IllegalArgumentException("ETK-R01: only complete confirmed orders issue tickets");Set<UUID> ids=new HashSet<>();for(var i:o.items())if(i.id()==null||i.ticketTypeId()==null||i.name()==null||i.name().isBlank()||i.quantity()<1||i.quantity()>10000||!ids.add(i.id()))throw new IllegalArgumentException("ETK-R02: invalid unique issuance unit");}
 @Transactional public void fulfill(UUID id){var rows=db.queryForList("select snapshot::text from ticket_admission.issuance_obligations where order_id=? and state='PENDING'",String.class,id);if(rows.isEmpty())return;
  Order order;try{order=json.readValue(rows.getFirst(),Order.class);}catch(Exception ex){throw new IllegalArgumentException("Invalid obligation",ex);}
  var current=source.order(id);validate(current);if(!current.id().equals(order.id())||!current.customerId().equals(order.customerId())||!current.eventId().equals(order.eventId())||!current.organizationId().equals(order.organizationId())||!current.items().equals(order.items()))throw new IllegalArgumentException("ETK-R02: authoritative units do not match");
  var event=source.event(order.eventId());if(!event.organizationId().equals(order.organizationId()))throw new IllegalArgumentException("Event organization mismatch");
  gates.snapshot(event);db.queryForList("select order_id from ticket_admission.issuance_obligations where order_id=? for update",id);
  if(gates.frozen(event.id())){if(db.update("update ticket_admission.issuance_obligations set state='CANCELLED',completed_at=clock_timestamp() where order_id=? and state='PENDING'",id)==1)facts.emit(id,"ticket.order.cancelled.v1",Map.of("orderId",id,"customerId",order.customerId(),"eventId",event.id()));return;}
  if(!"PENDING".equals(db.queryForObject("select state from ticket_admission.issuance_obligations where order_id=?",String.class,id)))return;
  var buyer=source.contact(order.customerId());if(!order.customerId().equals(buyer.userId()))throw new IllegalArgumentException("Buyer mismatch");
  for(var item:order.items())for(int ordinal=1;ordinal<=item.quantity();ordinal++){
   UUID ticket=UUID.nameUUIDFromBytes((id+":"+item.id()+":"+ordinal).getBytes(java.nio.charset.StandardCharsets.UTF_8));String raw=credentials.create();
   int inserted=db.update("insert into ticket_admission.tickets(id,order_id,item_id,ordinal,event_id,customer_id,ticket_type_id,ticket_type_name,buyer_name,buyer_email,credential_hash,credential_ciphertext) values (?,?,?,?,?,?,?,?,?,?,?,?) on conflict(order_id,item_id,ordinal) do nothing",ticket,id,item.id(),ordinal,event.id(),order.customerId(),item.ticketTypeId(),item.name(),buyer.displayName(),buyer.email(),Credentials.hash(raw),credentials.encrypt(ticket,raw));
   if(inserted==1)facts.ticket(ticket,"ticket.ticket.issued.v1");
  }
  db.update("update ticket_admission.issuance_obligations set state='READY',completed_at=clock_timestamp(),attempts=attempts+1 where order_id=?",id);
  facts.emit(id,"ticket.order.ready.v1",Map.of("orderId",id,"customerId",order.customerId(),"eventId",event.id(),"eventName",order.eventName()));
 }
}
