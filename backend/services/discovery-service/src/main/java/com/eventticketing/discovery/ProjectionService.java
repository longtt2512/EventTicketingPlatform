package com.eventticketing.discovery;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectionService {
    private final JdbcTemplate db;private final ObjectMapper json;
    public ProjectionService(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @Transactional public void apply(String body) {
        try {
            JsonNode envelope=json.readTree(body),payload=envelope.required("payload");
            if(envelope.required("eventVersion").asInt()!=1||!"event-commerce".equals(envelope.required("producer").asText()))throw new IllegalArgumentException("Unsupported event projection contract");
            UUID message=UUID.fromString(envelope.required("eventId").asText()),id=UUID.fromString(payload.required("id").asText());
            if(!id.toString().equals(envelope.required("aggregateId").asText()))throw new IllegalArgumentException("Aggregate mismatch");
            long version=payload.required("version").asLong(-1);if(version<0)throw new IllegalArgumentException("Missing version");
            if(db.update("insert into discovery.inbox(event_id) values (?) on conflict do nothing",message)==0)return;
            if(db.update("insert into discovery.projection_versions(id,version) values (?,?) on conflict(id) do update set version=excluded.version where projection_versions.version<excluded.version",id,version)==0)return;
            boolean published=payload.hasNonNull("publishedAt");String state=payload.required("status").asText();
            if(!published||!Set.of("PUBLISHED","ONGOING","COMPLETED","CANCELLED").contains(state)) {db.update("delete from discovery.public_events where id=?",id);return;}
            ((ObjectNode)payload).remove("reviewReason");
            long sold=0;for(JsonNode ticket:payload.required("ticketTypes"))sold+=ticket.required("sold").asLong();
            db.update("insert into discovery.public_events(id,name,category,city,start_at,end_at,status,featured,sold,payload,version,source_updated_at) values (?,?,?,?,?,?,?,?,?,?::jsonb,?,?) on conflict(id) do update set name=excluded.name,category=excluded.category,city=excluded.city,start_at=excluded.start_at,end_at=excluded.end_at,status=excluded.status,featured=excluded.featured,sold=excluded.sold,payload=excluded.payload,version=excluded.version,source_updated_at=excluded.source_updated_at,projected_at=clock_timestamp()",
                id,payload.required("name").asText(),payload.required("category").asText(),payload.required("city").asText(),timestamp(payload,"startAt"),timestamp(payload,"endAt"),state,payload.required("featured").asBoolean(),sold,json.writeValueAsString(payload),version,timestamp(payload,"updatedAt"));
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalArgumentException("Invalid event projection JSON",ex);}
    }
    private Timestamp timestamp(JsonNode node,String name){return Timestamp.from(Instant.parse(node.required(name).asText()));}
}
