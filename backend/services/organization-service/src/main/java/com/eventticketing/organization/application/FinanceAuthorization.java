package com.eventticketing.organization.application;

import com.eventticketing.organization.api.ApiException;
import com.eventticketing.organization.infrastructure.IdentityClient;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceAuthorization {
    private final JdbcTemplate db;private final OrganizationAccess access;private final IdentityClient identity;
    public FinanceAuthorization(JdbcTemplate db,OrganizationAccess access,IdentityClient identity){this.db=db;this.access=access;this.identity=identity;}
    public record Authorize(@NotNull UUID settlementId,@NotNull UUID userId,@NotNull UUID payoutId,
                            @Min(1) long destinationVersion,@NotBlank @Size(max=128)String fingerprint){}
    @Transactional(readOnly=true)
    public Map<String,Object> scope(UUID org,UUID actor,boolean mutation){
        var user=identity.securityState(actor);
        if(user==null||!"ACTIVE".equals(user.status())||!user.emailVerified())throw denied();
        if(!user.roles().contains("ADMIN"))access.require(org,actor,"FINANCE",mutation);
        var orgs=db.queryForList("select id,name,status,created_by_user_id,version from organization.organizations where id=?",org);
        if(orgs.isEmpty())throw OrganizationAccess.missing();var row=orgs.getFirst();
        if(mutation&&!"ACTIVE".equals(row.get("status")))throw new ApiException(HttpStatus.CONFLICT,"ORGANIZATION_SUSPENDED","Tổ chức bị tạm dừng; chưa thể đối soát.");
        var result=new LinkedHashMap<String,Object>(row);
        var payouts=db.queryForList("select id,destination_version,bank_name,account_holder,masked_account,status from organization.payout_destinations where organization_id=? and status='ACTIVE' order by destination_version desc limit 1",org);
        result.put("payout",payouts.isEmpty()?null:payouts.getFirst());return result;
    }
    @Transactional
    public Map<String,Object> authorize(UUID org,Authorize request){
        access.lock(org);var user=identity.securityState(request.userId());
        if(user==null||!"ACTIVE".equals(user.status())||!user.emailVerified()||!user.roles().contains("ADMIN"))throw denied();
        var old=db.queryForList("select * from organization.payout_authorizations where settlement_id=?",request.settlementId());
        if(!old.isEmpty()){
            var value=old.getFirst();if(!org.equals(value.get("organization_id"))||!request.fingerprint().equals(value.get("request_fingerprint"))||!request.payoutId().equals(value.get("payout_id"))||request.destinationVersion()!=((Number)value.get("destination_version")).longValue()||!request.userId().equals(value.get("actor_id")))throw conflict();return normalize(value);
        }
        var state=scope(org,request.userId(),true);Object value=state.get("payout");
        if(!(value instanceof Map<?,?> payout)||!request.payoutId().equals(payout.get("id"))||request.destinationVersion()!=((Number)payout.get("destination_version")).longValue())
            throw new ApiException(HttpStatus.CONFLICT,"PAYOUT_CHANGED","Tài khoản nhận tiền đã đổi hoặc chưa được xác minh. Kiểm tra lại snapshot trước khi trả.");
        db.update("insert into organization.payout_authorizations(settlement_id,organization_id,payout_id,destination_version,actor_id,request_fingerprint) values (?,?,?,?,?,?)",
            request.settlementId(),org,request.payoutId(),request.destinationVersion(),request.userId(),request.fingerprint());
        access.audit(org,null,request.userId(),"PAYOUT_AUTHORIZED",request.settlementId(),"Verified destination version "+request.destinationVersion());
        return normalize(db.queryForList("select * from organization.payout_authorizations where settlement_id=?",request.settlementId()).getFirst());
    }
    private Map<String,Object> normalize(Map<String,Object> input){var result=new LinkedHashMap<String,Object>();input.forEach((k,v)->result.put(k,v instanceof Timestamp t?t.toInstant():v));return result;}
    private ApiException denied(){return new ApiException(HttpStatus.FORBIDDEN,"FINANCE_PERMISSION_REQUIRED","Bạn không có quyền tài chính hiện hành.");}
    private ApiException conflict(){return new ApiException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","Đối soát đã có một quyết định với nội dung khác.");}
}
