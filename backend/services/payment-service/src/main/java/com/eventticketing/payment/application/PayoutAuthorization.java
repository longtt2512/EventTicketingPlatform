package com.eventticketing.payment.application;

import com.eventticketing.payment.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** A durable, replayable eligibility proof; Finance remains owner of the payout. */
@Service
public class PayoutAuthorization {
    private final JdbcTemplate db;private final FinancialLedger ledger;private final ObjectMapper json;private final RestClient organization;
    public PayoutAuthorization(JdbcTemplate db,FinancialLedger ledger,ObjectMapper json,
        @Value("${app.organization-base-url:http://localhost:8082}")String base,@Value("${app.identity.service-token}")String token){
        this.db=db;this.ledger=ledger;this.json=json;
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        organization=RestClient.builder().baseUrl(base).defaultHeader("X-Service-Token",token).requestFactory(factory).build();
    }
    public record Request(@NotNull UUID settlementId,@NotNull UUID eventId,@NotNull UUID organizationId,
        @NotNull UUID userId,@NotNull UUID payoutId,@Min(1)long destinationVersion,
        @NotBlank @Size(max=128)String paymentFingerprint,@NotBlank @Size(max=128)String evidenceFingerprint){}
    @Transactional
    public JsonNode authorize(Request request){
        ledger.lock(null);ledger.lock(request.eventId());String hash=ledger.hash(ledger.write(request));
        var old=db.queryForList("select request_hash,organization_authorization::text from payment.payout_authorizations where settlement_id=?",request.settlementId());
        if(!old.isEmpty()){
            if(!hash.equals(old.getFirst().get("request_hash")))throw conflict("IDEMPOTENCY_CONFLICT","Bằng chứng chi trả đã được dùng với nội dung khác.");
            return read((String)old.getFirst().get("organization_authorization"));
        }
        var state=ledger.snapshot(request.eventId());
        if(!request.paymentFingerprint().equals(state.get("fingerprint")))throw conflict("FINANCIAL_STATE_CHANGED","Thanh toán hoặc nghĩa vụ đã đổi. Tải lại đối soát để kiểm tra.");
        if(!((List<?>)state.get("openExceptions")).isEmpty())throw conflict("PAYMENT_EXCEPTION_OPEN","Còn ngoại lệ tài chính cần xử lý trước khi trả.");
        JsonNode proof;
        try{proof=organization.post().uri("/internal/organizations/{org}/finance/payout-authorizations",request.organizationId())
            .body(Map.of("settlementId",request.settlementId(),"userId",request.userId(),"payoutId",request.payoutId(),"destinationVersion",request.destinationVersion(),"fingerprint",request.evidenceFingerprint()))
            .retrieve().body(JsonNode.class);
        }catch(org.springframework.web.client.HttpClientErrorException ex){throw conflict("PAYOUT_AUTHORIZATION_CHANGED","Quyền hoặc tài khoản nhận tiền đã đổi. Kiểm tra lại trước khi trả.");}
        catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"PAYOUT_AUTHORIZATION_UNKNOWN","Chưa biết kết quả kiểm tra tài khoản. Giữ nguyên yêu cầu để thử lại an toàn.");}
        if(proof==null)throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"PAYOUT_AUTHORIZATION_UNKNOWN","Chưa nhận được bằng chứng xác minh.");
        db.update("insert into payment.payout_authorizations(settlement_id,event_id,organization_id,request_hash,payment_fingerprint,organization_authorization) values (?,?,?,?,?,?::jsonb)",
            request.settlementId(),request.eventId(),request.organizationId(),hash,request.paymentFingerprint(),proof.toString());
        return proof;
    }
    private JsonNode read(String value){try{return json.readTree(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private ApiException conflict(String code,String detail){return new ApiException(HttpStatus.CONFLICT,code,detail);}
}
