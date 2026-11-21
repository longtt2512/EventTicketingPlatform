package com.eventticketing.commerce.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class EventChanges {
    private final EventRepository repository;private final ObjectMapper json;private final OrderFacts orderFacts;private final OrderRepository orders;
    public EventChanges(EventRepository repository,ObjectMapper json,OrderFacts orderFacts,OrderRepository orders){this.repository=repository;this.json=json;this.orderFacts=orderFacts;this.orders=orders;}
    public void record(UUID id,UUID actor,String action,String reason) {
        var db=repository.db();
        db.update("update event_commerce.events set version=version+1,updated_at=clock_timestamp() where id=?",id);
        var e=repository.get(id);
        db.update("insert into event_commerce.event_audit(id,event_id,actor_id,action,reason,version) values (?,?,?,?,?,?)",UUID.randomUUID(),id,actor,action,reason==null?"":reason,e.version());
        if(action.equals("CANCELLED")||action.equals("CANCEL_REQUESTED")) {
            var cancelled=db.queryForList("update event_commerce.orders set status='CANCELLED',updated_at=clock_timestamp(),version=version+1 where event_id=? and status='PENDING_PAYMENT' returning id",UUID.class,id);
            for(UUID order:cancelled){orderFacts.audit(order,actor,"EVENT_CANCELLED");orderFacts.emit(order,"cancelled",orders.get(order));}
            if(action.equals("CANCELLED"))db.update("update event_commerce.orders set payment_status='EXCEPTION',updated_at=clock_timestamp(),version=version+1 where event_id=? and status='CONFIRMED' and total_vnd>0 and payment_status='SUCCESS'",id);
        }
        if(Set.of("APPROVED","REJECTED","CANCELLED").contains(action))
            db.update("insert into event_commerce.notification_outbox(id,payload) values (?,jsonb_build_object('kind',?::text,'organizationId',?::text,'eventId',?::text,'name',?::text,'reason',?::text))",UUID.randomUUID(),action,e.organizationId().toString(),id.toString(),e.name(),reason==null?"":reason);
        UUID messageId=UUID.randomUUID();
        String type=switch(action){case "PUBLISHED"->"published";case "CANCELLED"->"cancelled";case "APPROVED"->"approved";default->"updated";};
        var payload=json.valueToTree(e);
        ((com.fasterxml.jackson.databind.node.ObjectNode)payload).remove("reviewReason");
        var envelope=Map.of("eventId",messageId,"eventType","event."+type,"eventVersion",1,"occurredAt",Instant.now(),"producer","event-commerce","aggregateType","event","aggregateId",id,"correlationId",messageId,"causationId",messageId,"payload",payload);
        try {db.update("insert into event_commerce.event_outbox(id,aggregate_id,topic,body) values (?,?,?,?::jsonb)",messageId,id,"event.event."+type+".v1",json.writeValueAsString(envelope));}
        catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}
    }
}
