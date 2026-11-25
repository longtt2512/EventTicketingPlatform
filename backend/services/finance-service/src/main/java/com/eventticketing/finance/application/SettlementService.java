package com.eventticketing.finance.application;

import com.eventticketing.finance.api.ApiException;
import com.eventticketing.finance.infrastructure.FinanceSources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
    private final SettlementRepository repo;private final SettlementEligibility eligibility;
    private final FinanceSources sources;private final FinanceFacts facts;private final ObjectMapper json;
    public SettlementService(SettlementRepository repo,SettlementEligibility eligibility,FinanceSources sources,FinanceFacts facts,ObjectMapper json){this.repo=repo;this.eligibility=eligibility;this.sources=sources;this.facts=facts;this.json=json;}
    public record Request(@NotNull UUID eventId){}
    public record Reason(@NotBlank @Size(min=10,max=2000)String reason){}
    public record Evidence(@NotNull @Pattern(regexp="PAID|NO_TRANSFER")String kind,@Min(0)long amountVnd,
        @Size(max=120)String reference,@NotNull Instant occurredAt,@NotBlank @Size(min=10,max=2000)String evidence){}

    public ObjectNode preview(UUID org,UUID event,UUID actor){return eligibility.evaluate(org,event,actor,false);}
    public ObjectNode read(UUID org,UUID id,UUID actor){sources.organization(org,actor,false);var value=repo.get(id);if(!org.toString().equals(value.path("organizationId").asText()))throw SettlementRepository.missing();value.set("history",json.valueToTree(repo.history(id)));return value;}
    public Map<String,Object> list(UUID org,UUID actor,String status,UUID cursor,int limit){if(org!=null)sources.organization(org,actor,false);return repo.list(org,status,cursor,limit);}
    public JsonNode eligibleEvents(UUID org,UUID actor){sources.organization(org,actor,false);return sources.events(org);}
    @Transactional
    public ObjectNode request(UUID org,UUID actor,String key,Request request){
        key(key);sources.organization(org,actor,true);
        repo.db().queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","settlement-request:"+actor+":"+key);
        var old=repo.db().queryForList("select event_id,settlement_id from finance.settlement_requests where actor_id=? and request_key=?",actor,key);
        if(!old.isEmpty()){
            if(!request.eventId().equals(old.getFirst().get("event_id")))throw conflict("IDEMPOTENCY_CONFLICT","Mã yêu cầu đã dùng cho sự kiện khác.");
            return read(org,(UUID)old.getFirst().get("settlement_id"),actor);
        }
        repo.db().queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","settlement-event:"+request.eventId());
        if(!repo.db().queryForList("select id from finance.settlements where event_id=?",request.eventId()).isEmpty())throw conflict("ALREADY_CLAIMED","Sự kiện đã có batch đối soát. Mở batch hiện tại để tiếp tục.");
        var snapshot=eligibility.evaluate(org,request.eventId(),actor,true);requireEligible(snapshot);UUID id=UUID.randomUUID();
        repo.db().update("insert into finance.settlements(id,organization_id,event_id,status,snapshot,requested_by) values (?,?,?,'PENDING',?::jsonb,?)",id,org,request.eventId(),snapshot.toString(),actor);
        for(var item:snapshot.path("items"))repo.db().update("insert into finance.settlement_items(order_id,settlement_id,amount_vnd) values (?,?,?)",UUID.fromString(item.path("orderId").asText()),id,item.path("grossVnd").longValue());
        repo.db().update("insert into finance.settlement_requests(actor_id,request_key,event_id,settlement_id) values (?,?,?,?)",actor,key,request.eventId(),id);
        var value=repo.get(id);facts.audit(id,actor,"SETTLEMENT_REQUESTED",null,value,"Yêu cầu đối soát toàn bộ sự kiện");facts.emit(id,"updated",value);return value;
    }
    @Transactional
    public ObjectNode process(UUID id,UUID actor,long version,String reason){
        var old=repo.lock(id);version(old,version);String status=old.path("status").asText();
        if(!Set.of("PENDING","FAILED").contains(status)||("FAILED".equals(status)&&!"NOT_PAID".equals(old.path("transferStatus").asText())))throw conflict("INVALID_SETTLEMENT_STATE","Chỉ tiếp tục batch mới hoặc lần chuyển chắc chắn chưa trả.");
        if(!old.path("decision").isNull())throw conflict("TRANSFER_UNKNOWN","Còn bằng chứng đang xác minh. Không được chỉ dẫn trả lần hai.");
        var fresh=refresh(old,actor);sameAmounts(old.path("snapshot"),fresh);sameDestination(old.path("snapshot"),fresh);
        repo.db().update("update finance.settlements set status='PROCESSING',snapshot=?::jsonb,transfer_status='NOT_STARTED',version=version+1 where id=?",fresh.toString(),id);
        return changed(id,actor,"SETTLEMENT_PROCESSING",old,reason);
    }
    @Transactional
    public ObjectNode destination(UUID id,UUID actor,long version,String reason){
        var old=repo.lock(id);version(old,version);
        if(!Set.of("PENDING","FAILED").contains(old.path("status").asText())||!Set.of("NOT_STARTED","NOT_PAID").contains(old.path("transferStatus").asText())||!old.path("decision").isNull())throw conflict("TRANSFER_UNKNOWN","Không đổi snapshot tài khoản khi đã trả hoặc chưa rõ kết quả.");
        var fresh=refresh(old,actor);sameAmounts(old.path("snapshot"),fresh);
        repo.db().update("update finance.settlements set snapshot=?::jsonb,version=version+1 where id=?",fresh.toString(),id);return changed(id,actor,"PAYOUT_SNAPSHOT_REFRESHED",old,reason);
    }
    @Transactional
    public ObjectNode fail(UUID id,UUID actor,long version,String reason,boolean unknown){
        var old=repo.lock(id);version(old,version);
        if(!"PROCESSING".equals(old.path("status").asText())||!old.path("decision").isNull())throw conflict("TRANSFER_UNKNOWN","Có quyết định chi trả đang xác minh hoặc trạng thái không phù hợp. Giữ batch để phục hồi.");
        repo.db().update("update finance.settlements set status=?,transfer_status=?,version=version+1 where id=?",unknown?"PROCESSING":"FAILED",unknown?"UNKNOWN":"NOT_PAID",id);
        return changed(id,actor,unknown?"TRANSFER_UNKNOWN":"TRANSFER_NOT_PAID",old,reason);
    }
    @Transactional
    public ObjectNode prepare(UUID id,UUID actor,long version,Evidence evidence){
        var old=repo.lock(id);JsonNode input=json.valueToTree(evidence);
        if(!old.path("decision").isNull()){
            if(!same(input,old.path("decision")))throw conflict("IDEMPOTENCY_CONFLICT","Batch đã lưu bằng chứng khác. Không gửi một lần chuyển mới.");
            return old;
        }
        version(old,version);if(!"PROCESSING".equals(old.path("status").asText()))throw conflict("INVALID_SETTLEMENT_STATE","Cần kiểm tra và chuyển batch sang đang xử lý trước.");
        var fresh=refresh(old,actor);sameAmounts(old.path("snapshot"),fresh);sameDestination(old.path("snapshot"),fresh);
        long net=fresh.path("netVnd").longValue();
        if(evidence.amountVnd()!=net||evidence.occurredAt().isAfter(Instant.now())||evidence.occurredAt().isBefore(Instant.parse(old.path("requestedAt").asText())))throw CommissionService.invalid("Bằng chứng phải khớp số tiền và ngày từ lúc yêu cầu đến hiện tại.");
        boolean zero=net==0;
        if(zero?(!"NO_TRANSFER".equals(evidence.kind())||(evidence.reference()!=null&&!evidence.reference().isBlank())):(!"PAID".equals(evidence.kind())||evidence.reference()==null||!evidence.reference().matches("MOCK-SETTLEMENT-[A-Za-z0-9-]{6,80}")))
            throw CommissionService.invalid("Số tiền 0 cần quyết định không chuyển tiền. Khoản trả cần mã MOCK-SETTLEMENT-… và bằng chứng mô phỏng.");
        repo.db().update("update finance.settlements set snapshot=?::jsonb,decision=?::jsonb,decision_actor=?,transfer_status='UNKNOWN',transfer_reference=?,version=version+1 where id=?",
            fresh.toString(),input.toString(),actor,zero?null:evidence.reference(),id);
        return changed(id,actor,"TRANSFER_EVIDENCE_RECORDED",old,evidence.evidence());
    }
    private ObjectNode refresh(JsonNode old,UUID actor){var fresh=eligibility.evaluate(UUID.fromString(old.path("organizationId").asText()),UUID.fromString(old.path("eventId").asText()),actor,true);requireEligible(fresh);return fresh;}
    private void requireEligible(JsonNode value){if(!value.path("eligible").asBoolean())throw conflict("SETTLEMENT_NOT_ELIGIBLE","Chưa đủ điều kiện đối soát: "+value.path("reasons").toString());}
    private void sameAmounts(JsonNode old,JsonNode fresh){for(String name:List.of("grossVnd","platformFeeVnd","gatewayFeeVnd","adjustmentsVnd","netVnd","items"))if(!same(old.path(name),fresh.path(name)))throw conflict("FINANCIAL_STATE_CHANGED","Snapshot tài chính đã thay đổi. Cần kiểm tra trước khi chi trả.");}
    private void sameDestination(JsonNode old,JsonNode fresh){for(String name:List.of("id","destination_version"))if(!same(old.path("payout").path(name),fresh.path("payout").path(name)))throw conflict("PAYOUT_CHANGED","Tài khoản đã đổi. Cập nhật snapshot có kiểm chứng trước khi tiếp tục.");}
    private ObjectNode changed(UUID id,UUID actor,String action,ObjectNode before,String reason){var value=repo.get(id);facts.audit(id,actor,action,before,value,reason);facts.emit(id,"updated",value);return value;}
    private boolean same(JsonNode left,JsonNode right){return left.equals((a,b)->a.isNumber()&&b.isNumber()?a.decimalValue().compareTo(b.decimalValue()):a.equals(b)?0:1,right);}
    private void version(JsonNode value,long expected){if(value.path("version").longValue()!=expected)throw conflict("STALE_VERSION","Đối soát đã thay đổi. Tải lại và kiểm tra trước khi tiếp tục.");}
    private void key(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{8,128}"))throw CommissionService.invalid("Mã yêu cầu không hợp lệ.");}
    private ApiException conflict(String code,String message){return new ApiException(HttpStatus.CONFLICT,code,message);}
}
