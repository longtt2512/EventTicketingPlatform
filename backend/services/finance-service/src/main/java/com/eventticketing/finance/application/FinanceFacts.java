package com.eventticketing.finance.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FinanceFacts {
    private final JdbcTemplate db;private final ObjectMapper json;
    public FinanceFacts(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    public void audit(UUID resource,UUID actor,String action,Object before,Object after,String reason){
        db.update("insert into finance.audit(id,resource_id,actor_id,action,before_value,after_value,reason) values (?,?,?,?,?::jsonb,?::jsonb,?)",
            UUID.randomUUID(),resource,actor,action,write(before),write(after),reason);
    }
    public void emit(UUID id,String type,Object payload){
        UUID message=UUID.randomUUID();var envelope=Map.of("eventId",message,"eventVersion",1,"eventType","finance."+type,
            "occurredAt",Instant.now(),"producer","finance","aggregateType","settlement","aggregateId",id,
            "correlationId",message,"causationId",message,"payload",payload);
        db.update("insert into finance.outbox(id,aggregate_id,topic,body) values (?,?,?,?::jsonb)",message,id,"finance.settlement."+type+".v1",write(envelope));
    }
    public String write(Object value){try{return json.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
}
