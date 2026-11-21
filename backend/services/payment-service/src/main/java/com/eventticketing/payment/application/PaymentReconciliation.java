package com.eventticketing.payment.application;

import static com.eventticketing.payment.application.PaymentRepository.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReconciliation {
 private final FinancialLedger financial;private final JdbcTemplate db;private final ReceiptService receipts;private final PaymentRepository repo;
 public PaymentReconciliation(JdbcTemplate db,ReceiptService receipts,PaymentRepository repo,FinancialLedger financial){this.financial=financial;this.db=db;this.receipts=receipts;this.repo=repo;}
 @Transactional public void apply(UUID messageId,String topic,JsonNode p){
  if(db.update("insert into payment.inbox(id) values (?) on conflict do nothing",messageId)==0)return;
  if(topic.equals("event.event.cancelled.v1")){
   UUID event=UUID.fromString(p.path("id").asText());financial.lock(event);
   var rows=db.queryForList("select t.*,a.expected_amount from payment.transactions t join payment.attempts a on a.id=t.attempt_id where t.event_id=? and t.status='SUCCESS' order by t.id for update of t",event);
   for(var t:rows)receipts.openCase((UUID)t.get("id"),(UUID)t.get("order_id"),event,"EVENT_CANCELLED",((Number)t.get("expected_amount")).longValue(),((Number)t.get("amount")).longValue(),(String)t.get("currency"));financial.changed(event);return;
  }
  UUID transaction=UUID.fromString(p.path("paymentId").asText());
  var eventIds=db.queryForList("select event_id from payment.transactions where id=?",UUID.class,transaction);
  if(!eventIds.isEmpty())financial.lock(eventIds.getFirst());
  var rows=db.queryForList("select * from payment.transactions where id=? for update",transaction);if(rows.isEmpty())throw new IllegalArgumentException("Payment outcome references an unknown transaction");
  var t=rows.getFirst();String result=p.path("result").asText();
  if(!Set.of("APPLIED","AMOUNT_MISMATCH","EVENT_CANCELLED","DUPLICATE_COLLECTION","LATE_SUCCESS","EVENT_UNAVAILABLE").contains(result))throw new IllegalArgumentException("Unknown confirmation result");
  db.update("update payment.transactions set result=? where id=?",result,transaction);
  db.update("update payment.attempts set result=?,updated_at=clock_timestamp() where id=?",result,t.get("attempt_id"));
  if(!result.equals("APPLIED"))receipts.openCase(transaction,(UUID)t.get("order_id"),(UUID)t.get("event_id"),result,p.path("expectedAmount").asLong(),((Number)t.get("amount")).longValue(),(String)t.get("currency"));
  repo.audit(transaction,null,"ORDER_RESULT",result);financial.changed((UUID)t.get("event_id"));
 }
}
