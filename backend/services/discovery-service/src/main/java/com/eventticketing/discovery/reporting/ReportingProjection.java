package com.eventticketing.discovery.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingProjection {
    private final JdbcTemplate db;private final ObjectMapper json;
    public ReportingProjection(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @Transactional
    public void accept(String topic,String body)throws Exception{
        var envelope=json.readTree(body);if(envelope.path("eventVersion").asInt()!=1)throw new IllegalArgumentException("Unsupported report fact version");
        UUID message=UUID.fromString(envelope.path("eventId").asText());var payload=(ObjectNode)envelope.path("payload").deepCopy();
        if(db.update("insert into discovery.report_inbox(id) values (?) on conflict do nothing",message)==0)return;
        String kind;UUID id,org=null,event=null;long version;
        if(topic.startsWith("event.event.")){
            kind="EVENT";id=uuid(payload,"id");event=id;org=uuid(payload,"organizationId");version=payload.path("version").longValue();
            payload.remove(List.of("reviewReason","terms","description","organizationDescription"));
        }else if(topic.equals("event.order.confirmed.v1")){
            if(!"CONFIRMED".equals(payload.path("status").asText()))throw new IllegalArgumentException("RPT-R06: only confirmed sales count");
            kind="ORDER";id=uuid(payload,"id");org=uuid(payload,"organizationId");event=uuid(payload,"eventId");version=payload.path("version").longValue();
            payload.remove(List.of("customerId","terms","serverTime","canPay","canCancel","canConfirmFree"));
        }else if(topic.equals("payment.payment.financial-updated.v1")){
            kind="PAYMENT";boolean unmapped="UNMAPPED".equals(payload.path("ledgerScope").asText());id=uuid(payload,unmapped?"ledgerId":"eventId");event=unmapped?null:id;version=payload.path("version").longValue();
            if(!payload.path("transactions").isArray()||!payload.path("returns").isArray())throw new IllegalArgumentException("RPT-R07: missing payment evidence");
        }else if(topic.startsWith("finance.settlement.")){
            kind="SETTLEMENT";id=uuid(payload,"id");org=uuid(payload,"organizationId");event=uuid(payload,"eventId");version=payload.path("version").longValue();
            payload.remove(List.of("decision","authorization_proof","decisionActor","requestedBy"));
            if(payload.path("snapshot") instanceof ObjectNode snapshot)snapshot.remove("payout");
        }else throw new IllegalArgumentException("Unknown reporting topic");
        upsert(kind,id,org,event,version,payload);
    }
    public void upsert(String kind,UUID id,UUID org,UUID event,long version,JsonNode payload){
        db.update("insert into discovery.report_entities(kind,id,organization_id,event_id,version,payload) values (?,?,?,?,?,?::jsonb) on conflict(kind,id) do update set payload=excluded.payload,organization_id=excluded.organization_id,event_id=excluded.event_id,version=excluded.version,projected_at=clock_timestamp() where report_entities.version<excluded.version",
            kind,id,org,event,version,payload.toString());
    }
    private UUID uuid(JsonNode payload,String field){return UUID.fromString(payload.required(field).asText());}
}
