package com.eventticketing.payment.application;

import static com.eventticketing.payment.application.PaymentModels.*;
import static com.eventticketing.payment.application.PaymentRepository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persistent local provider. Only this boundary manufactures payment outcomes. */
@Service @ConditionalOnProperty(name="app.mock.enabled",havingValue="true")
public class MockProvider {
 private final JdbcTemplate db;private final PaymentRepository repo;private final ReceiptService receipts;private final MockProperties config;private final ObjectMapper json;
 public MockProvider(JdbcTemplate db,PaymentRepository repo,ReceiptService receipts,MockProperties config,ObjectMapper json){this.db=db;this.repo=repo;this.receipts=receipts;this.config=config;this.json=json;}
 @Transactional public Attempt choose(UUID id,UUID actor,String scenario){return choose(id,actor,scenario,false);}
 @Transactional public Attempt choose(UUID id,UUID actor,String scenario,boolean admin){
  repo.owner(id,actor,admin);var a=repo.get(id);var row=db.queryForList("select * from payment.mock_sessions where attempt_id=? for update",id).getFirst();
  if(row.get("scenario")!=null&&!"UNKNOWN".equals(row.get("scenario"))){if(scenario.equals(row.get("scenario")))return a;throw invalid("Kịch bản đã được chọn. Kiểm tra kết quả giao dịch trước khi tiếp tục.");}
  Instant available=switch(scenario){case "DELAYED"->Instant.now().plusSeconds(15);case "LATE_SUCCESS"->a.expiresAt().plusSeconds(2);default->Instant.now();};
  String state=scenario.equals("FAILED")?"FAILED":scenario.equals("UNKNOWN")?"PENDING":"SUCCESS";
  long amount=a.expectedAmount()+(scenario.equals("WRONG_AMOUNT")?1000:0);
  db.update("update payment.mock_sessions set scenario=?,status=?,received_amount=?,available_at=?,paid_at=?,duplicate_delivery=? where attempt_id=?",scenario,state,amount,scenario.equals("UNKNOWN")?null:Timestamp.from(available),state.equals("SUCCESS")?Timestamp.from(available):null,scenario.equals("DUPLICATE"),id);
  repo.audit(id,actor,"MOCK_SCENARIO",scenario);return repo.get(id);
 }
 public void query(UUID id){deliver(id,true);}
 public void deliver(UUID id,boolean query){
  var rows=db.queryForList("select m.*,a.order_id,a.currency from payment.mock_sessions m join payment.attempts a on a.id=m.attempt_id where m.attempt_id=?",id);if(rows.isEmpty())return;var m=rows.getFirst();
  if("PENDING".equals(m.get("status"))||m.get("available_at")==null||((Timestamp)m.get("available_at")).toInstant().isAfter(Instant.now())||(!query&&"LOST_CALLBACK".equals(m.get("scenario"))))return;
  try{
   // A query verifies the same provider contract as a callback; it never infers success from a browser return.
   int deliveries=Boolean.TRUE.equals(m.get("duplicate_delivery"))?2:1;
   for(int i=0;i<deliveries;i++){var r=new Receipt(UUID.randomUUID(),"local-mock",config.merchant(),id,(UUID)m.get("order_id"),(String)m.get("reference"),((Number)m.get("received_amount")).longValue(),(String)m.get("currency"),(String)m.get("status"),Instant.now(),m.get("paid_at")==null?null:((Timestamp)m.get("paid_at")).toInstant());String body=json.writeValueAsString(r);receipts.receive(body,receipts.sign(body));}
   db.update("update payment.mock_sessions set delivered_at=clock_timestamp() where attempt_id=?",id);
  }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}
 }
}
