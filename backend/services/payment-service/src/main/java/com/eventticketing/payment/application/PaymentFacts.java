package com.eventticketing.payment.application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
public class PaymentFacts {
 private final JdbcTemplate db;private final ObjectMapper json;
 public PaymentFacts(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public void emit(UUID orderId,String type,Object payload){UUID id=UUID.randomUUID();try{var body=json.writeValueAsString(Map.of("eventId",id,"eventType","payment."+type,"eventVersion",1,"occurredAt",Instant.now(),"producer","payment","aggregateId",orderId,"aggregateType","order","correlationId",id,"causationId",id,"payload",payload));db.update("insert into payment.outbox(id,aggregate_id,topic,body) values (?,?,?,?::jsonb)",id,orderId,"payment.payment."+type+".v1",body);}catch(Exception ex){throw new IllegalStateException(ex);}}
}
