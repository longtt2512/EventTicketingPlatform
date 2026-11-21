package com.eventticketing.finance.application;

import com.eventticketing.finance.infrastructure.FinanceSources;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementCompletion {
    private final SettlementRepository repo;private final FinanceSources sources;private final FinanceFacts facts;
    public SettlementCompletion(SettlementRepository repo,FinanceSources sources,FinanceFacts facts){this.repo=repo;this.sources=sources;this.facts=facts;}
    @Transactional
    public ObjectNode complete(UUID id){
        var old=repo.lock(id);if("SETTLED".equals(old.path("status").asText())||old.path("decision").isNull())return old;
        var snapshot=old.path("snapshot");var payout=snapshot.path("payout");
        var request=Map.of("settlementId",id,"eventId",old.path("eventId").asText(),"organizationId",old.path("organizationId").asText(),
            "userId",old.path("decisionActor").asText(),"payoutId",payout.path("id").asText(),"destinationVersion",payout.path("destination_version").longValue(),
            "paymentFingerprint",snapshot.path("paymentFingerprint").asText(),"evidenceFingerprint",hash(old.path("decision").toString()));
        var proof=sources.authorize(request);var evidence=old.path("decision");
        repo.db().update("update finance.settlements set status='SETTLED',transfer_status=?,authorization_proof=?::jsonb,completed_at=?,version=version+1 where id=?",
            evidence.path("kind").asText(),proof.toString(),Timestamp.from(Instant.parse(evidence.path("occurredAt").asText())),id);
        var value=repo.get(id);facts.audit(id,UUID.fromString(old.path("decisionActor").asText()),"SETTLEMENT_COMPLETED",old,value,evidence.path("evidence").asText());
        repo.db().update("insert into finance.notification_outbox(id,payload) values (?,jsonb_build_object('kind','SETTLED','organizationId',?::text,'eventId',?::text,'resourceId',?::text,'name','Đối soát sự kiện','amount',?::bigint,'transferKind',?::text))",UUID.randomUUID(),old.path("organizationId").asText(),old.path("eventId").asText(),id.toString(),snapshot.path("netVnd").longValue(),evidence.path("kind").asText());
        facts.emit(id,"completed",value);return value;
    }
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
