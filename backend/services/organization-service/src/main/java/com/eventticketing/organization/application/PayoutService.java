package com.eventticketing.organization.application;
import com.eventticketing.organization.api.*;
import com.eventticketing.organization.api.OrganizationContracts.*;
import com.eventticketing.platform.SecretCipher;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PayoutService {
    private final JdbcTemplate db;private final OrganizationAccess access;private final SecretCipher cipher;
    public PayoutService(JdbcTemplate db,OrganizationAccess access,@Value("${app.data-encryption-key}") String key) {this.db=db;this.access=access;this.cipher=new SecretCipher(key);}
    @Transactional(readOnly=true)
    public List<Payout> list(UUID org,UUID actor,boolean admin) {
        if(!admin) access.require(org,actor,"FINANCE",false);
        return db.query("select * from organization.payout_destinations where organization_id=? order by destination_version desc",(r,n)->new Payout(r.getObject("id",UUID.class),r.getLong("destination_version"),r.getString("bank_name"),r.getString("account_holder"),r.getString("masked_account"),r.getString("status"),r.getString("review_reason")),org);
    }
    @Transactional
    public List<Payout> add(UUID org,UUID actor,PayoutInput request) {
        access.lock(org);access.owner(org,actor);
        UUID id=UUID.randomUUID();long version=db.queryForObject("select coalesce(max(destination_version),0)+1 from organization.payout_destinations where organization_id=?",Long.class,org);
        db.update("insert into organization.payout_destinations(id,organization_id,destination_version,bank_name,account_holder,masked_account,encrypted_details,status) values (?,?,?,?,?,?,?,'PENDING_REVERIFY')",id,org,version,request.bankName().trim(),request.accountHolder().trim(),"•••• "+request.accountNumber().substring(request.accountNumber().length()-4),cipher.encrypt(request.accountNumber(),org+":"+id));
        access.audit(org,null,actor,"PAYOUT_SUBMITTED",id,null);return list(org,actor,false);
    }
    @Transactional
    public List<Payout> review(UUID org,UUID id,UUID admin,boolean approve,String reason) {
        access.lock(org);
        if(!"ACTIVE".equals(db.queryForObject("select status from organization.organizations where id=?",String.class,org))) throw new ApiException(HttpStatus.CONFLICT,"ORGANIZATION_SUSPENDED","Restore the organization before reviewing payout destinations.");
        var rows=db.queryForList("select status from organization.payout_destinations where id=? and organization_id=?",String.class,id,org);
        if(rows.isEmpty()) throw OrganizationAccess.missing();
        if(!"PENDING_REVERIFY".equals(rows.getFirst())) throw new ApiException(HttpStatus.CONFLICT,"INVALID_PAYOUT_STATE","Only a pending destination can be reviewed.");
        if(approve) db.update("update organization.payout_destinations set status='REVOKED' where organization_id=? and status='ACTIVE'",org);
        db.update("update organization.payout_destinations set status=?,reviewed_by=?,reviewed_at=now(),review_reason=? where id=?",approve?"ACTIVE":"REJECTED",admin,reason,id);
        access.audit(org,null,admin,approve?"PAYOUT_APPROVED":"PAYOUT_REJECTED",id,reason);return list(org,admin,true);
    }
}
