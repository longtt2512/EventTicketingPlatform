package com.eventticketing.finance.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceProjection {
    private final JdbcTemplate db;private final ObjectMapper json;
    public FinanceProjection(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @Transactional
    public void accept(String topic,String body)throws Exception{
        var envelope=json.readTree(body);if(envelope.path("eventVersion").asInt()!=1)throw new IllegalArgumentException("Unsupported financial fact version");
        var payload=envelope.path("payload");UUID id=UUID.fromString(envelope.path("eventId").asText());
        if(db.update("insert into finance.inbox(id) values (?) on conflict do nothing",id)==0)return;
        if(topic.equals("event.order.confirmed.v1")){
            if(!"CONFIRMED".equals(payload.path("status").asText())||!payload.path("totalVnd").canConvertToLong()||payload.path("totalVnd").longValue()<0)throw new IllegalArgumentException("FIN-R02: invalid confirmed order fact");
            db.update("insert into finance.confirmed_orders(order_id,organization_id,event_id,payload,confirmed_at,source_version) values (?,?,?,?::jsonb,?,?) on conflict(order_id) do update set payload=excluded.payload,source_version=excluded.source_version where confirmed_orders.source_version<excluded.source_version",
                UUID.fromString(payload.path("id").asText()),UUID.fromString(payload.path("organizationId").asText()),UUID.fromString(payload.path("eventId").asText()),payload.toString(),Timestamp.from(Instant.parse(payload.path("confirmedAt").asText())),payload.path("version").longValue());
        }else if(topic.equals("payment.payment.financial-updated.v1")){
            if(!payload.path("transactions").isArray()||!payload.path("returns").isArray()||!payload.path("version").canConvertToLong())throw new IllegalArgumentException("FIN-R06: incomplete financial evidence");
            if("UNMAPPED".equals(payload.path("ledgerScope").asText()))return; // Eligibility reads the current global fence from Payment.
            db.update("insert into finance.financial_events(event_id,payload,source_version) values (?,?::jsonb,?) on conflict(event_id) do update set payload=excluded.payload,source_version=excluded.source_version,projected_at=clock_timestamp() where financial_events.source_version<excluded.source_version",
                UUID.fromString(payload.path("eventId").asText()),payload.toString(),payload.path("version").longValue());
        }else throw new IllegalArgumentException("Unknown financial fact topic");
    }
}
