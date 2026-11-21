package com.eventticketing.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Payment-owned evidence. Missing reconciliation is deliberately not a zero fee. */
@Service
public class FinancialLedger {
    private final JdbcTemplate db; private final ObjectMapper json; private final PaymentFacts facts;
    public FinancialLedger(JdbcTemplate db,ObjectMapper json,PaymentFacts facts){this.db=db;this.json=json;this.facts=facts;}
    public void lock(UUID event){
        db.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","finance-ledger:"+(event==null?"unmapped":event));
    }
    @Transactional(readOnly=true)
    public Map<String,Object> snapshot(UUID event){
        var result=new LinkedHashMap<String,Object>();result.put("eventId",event);
        result.put("transactions",db.queryForList("select t.id,t.order_id,t.reference,t.amount,t.currency,t.status,t.result,t.paid_at,r.fee_vnd,r.provider_reference,r.provenance,r.reconciled_at from payment.transactions t left join payment.gateway_reconciliations r on r.transaction_id=t.id where t.event_id is not distinct from ? order by t.id",event).stream().map(PaymentAdmin::normalize).toList());
        result.put("returns",db.queryForList("select r.id,r.reference,r.amount,r.currency,r.returned_at,e.transaction_id,e.order_id from payment.return_evidence r join payment.exceptions e on e.id=r.exception_id where e.event_id is not distinct from ? order by r.id",event).stream().map(PaymentAdmin::normalize).toList());
        result.put("attempts",db.queryForList("select id,order_id,expected_amount,status,result,created_at,updated_at,outcome_at from payment.attempts where event_id=? order by id",event).stream().map(PaymentAdmin::normalize).toList());
        result.put("openExceptions",db.queryForList("select id,order_id,transaction_id,type,received_amount,returned_amount,currency from payment.exceptions where status<>'RESOLVED' and (event_id=? or event_id is null) order by id",event).stream().map(PaymentAdmin::normalize).toList());
        result.put("fingerprint",hash(write(result)));return result;
    }
    public void changed(UUID event){
        if(event==null){
            var state=db.queryForMap("update payment.global_financial_state set version=version+1 where singleton=1 returning ledger_id,version");
            var unmapped=snapshot(null);unmapped.put("ledgerScope","UNMAPPED");unmapped.put("ledgerId",state.get("ledger_id"));unmapped.put("version",state.get("version"));
            facts.emit((UUID)state.get("ledger_id"),"financial-updated",unmapped);
            for(UUID affected:db.queryForList("select event_id from payment.financial_versions order by event_id",UUID.class)){lock(affected);changed(affected);}return;
        }
        db.update("insert into payment.financial_versions(event_id,version) values (?,1) on conflict(event_id) do update set version=financial_versions.version+1",event);
        var result=snapshot(event);result.put("version",db.queryForObject("select version from payment.financial_versions where event_id=?",Long.class,event));
        facts.emit(event,"financial-updated",result);
    }
    @Transactional
    public void bootstrapUnmapped(){lock(null);if(db.queryForObject("select version from payment.global_financial_state where singleton=1",Long.class)==0)changed(null);}
    @Transactional
    public int reconcile(){
        var rows=db.queryForList("select t.id,t.event_id,t.reference from payment.transactions t join payment.mock_sessions m on m.attempt_id=t.attempt_id and m.reference=t.reference and m.received_amount=t.amount join payment.attempts a on a.id=t.attempt_id and a.currency=t.currency where t.provider='local-mock' and t.status in ('SUCCESS','REFUNDED') and m.status='SUCCESS' and m.available_at<=clock_timestamp() and not exists(select 1 from payment.gateway_reconciliations r where r.transaction_id=t.id) order by t.event_id,t.id limit 100");
        int count=0;
        for(var row:rows){UUID event=(UUID)row.get("event_id");lock(event);
            int changed=db.update("insert into payment.gateway_reconciliations(transaction_id,fee_vnd,provider_reference,provenance) values (?,0,?,'LOCAL_PROVIDER_LEDGER') on conflict(transaction_id) do nothing",row.get("id"),row.get("reference"));
            if(changed>0){changed(event);count++;}
        }
        for(UUID event:db.queryForList("select distinct event_id from payment.attempts a where not exists(select 1 from payment.financial_versions v where v.event_id=a.event_id) order by event_id limit 100",UUID.class)){
            lock(event);changed(event);
        }
        return count;
    }
    public String write(Object value){try{return json.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    public String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
