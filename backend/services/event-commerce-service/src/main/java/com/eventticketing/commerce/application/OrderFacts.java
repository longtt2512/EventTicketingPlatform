package com.eventticketing.commerce.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderFacts {
 private final JdbcTemplate db;private final ObjectMapper json;
 public OrderFacts(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public void audit(UUID id,UUID actor,String action){db.update("insert into event_commerce.order_audit(id,order_id,actor_id,action) values (?,?,?,?)",UUID.randomUUID(),id,actor,action);}
 public void emit(UUID orderId,String type,Object payload){
  UUID id=UUID.randomUUID();
  try {
   if(payload instanceof OrderModels.Order){
    com.fasterxml.jackson.databind.node.ObjectNode enriched=json.valueToTree(payload);
    var row=db.queryForList("select commission_snapshot::text,applied_payment_id from event_commerce.orders where id=?",orderId).getFirst();
    if(row.get("commission_snapshot")!=null)enriched.set("commissionSnapshot",json.readTree((String)row.get("commission_snapshot")));
    if(row.get("applied_payment_id")!=null)enriched.put("appliedPaymentId",row.get("applied_payment_id").toString());
    payload=enriched;
   }
   String body=json.writeValueAsString(Map.of("eventId",id,"eventType","order."+type,"eventVersion",1,"occurredAt",Instant.now(),"producer","event-commerce","aggregateId",orderId,"aggregateType","order","correlationId",id,"causationId",id,"payload",payload));
   db.update("insert into event_commerce.event_outbox(id,aggregate_id,topic,body) values (?,?,?,?::jsonb)",id,orderId,"event.order."+type+".v1",body);
  }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}
 }
}
