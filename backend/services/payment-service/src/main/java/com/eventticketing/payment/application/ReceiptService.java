package com.eventticketing.payment.application;

import static com.eventticketing.payment.application.PaymentModels.*;
import static com.eventticketing.payment.application.PaymentRepository.*;
import com.eventticketing.payment.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {
 private final JdbcTemplate db;private final ObjectMapper json;private final Validator validator;
 private final FinancialLedger financial;private final MockProperties config;private final PaymentFacts facts;private final PaymentRepository repo;
 public ReceiptService(JdbcTemplate db,ObjectMapper json,Validator validator,MockProperties config,PaymentFacts facts,PaymentRepository repo,FinancialLedger financial){this.financial=financial;this.db=db;this.json=json;this.validator=validator;this.config=config;this.facts=facts;this.repo=repo;}
 public String sign(String body){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(config.secret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
 @Transactional public void receive(String body,String signature){
  if(!config.enabled()||body.length()>16384||signature==null||!MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_PAYMENT_SIGNATURE","Chữ ký thanh toán không hợp lệ.");
  Receipt r;try{r=json.readValue(body,Receipt.class);}catch(Exception ex){throw invalid("Nội dung callback không hợp lệ.");}
  if(!validator.validate(r).isEmpty()||!"local-mock".equals(r.provider())||!config.merchant().equals(r.merchant())||Duration.between(r.deliveredAt(),Instant.now()).abs().toSeconds()>300)throw invalid("Callback sai nhà cung cấp, người nhận hoặc thời gian.");
  db.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))",r.provider()+"|"+r.merchant()+"|"+r.reference());
  String fingerprint=sign(body);var prior=db.queryForList("select payload_hash from payment.receipts where id=?",String.class,r.deliveryId());
  if(!prior.isEmpty()){if(!prior.getFirst().equals(fingerprint))throw invalid("Callback ID đã dùng với nội dung khác.");return;}
  db.update("insert into payment.receipts(id,payload_hash,reference) values (?,?,?)",r.deliveryId(),fingerprint,r.reference());
  var mapping=db.queryForList("select event_id,order_id from payment.attempts where id=?",r.attemptId());
  boolean matches=!mapping.isEmpty()&&r.orderId().equals(mapping.getFirst().get("order_id"));
  financial.lock(matches?(UUID)mapping.getFirst().get("event_id"):null);
  List<Map<String,Object>> attempts=matches?db.queryForList("select * from payment.attempts where id=? for update",r.attemptId()):List.of();
  Map<String,Object> a=attempts.isEmpty()?null:attempts.getFirst();
  boolean mapped=a!=null&&r.orderId().equals(a.get("order_id"));
  var transactions=db.queryForList("select * from payment.transactions where provider=? and merchant=? and reference=? for update",r.provider(),r.merchant(),r.reference());
  UUID transaction=transactions.isEmpty()?UUID.randomUUID():(UUID)transactions.getFirst().get("id");
  if(!transactions.isEmpty()&&Set.of("SUCCESS","REFUNDED").contains(transactions.getFirst().get("status"))){repo.audit(transaction,null,"CALLBACK_REPLAY","Verified duplicate/conflicting delivery retained");return;}
  if(transactions.isEmpty())db.update("insert into payment.transactions(id,attempt_id,order_id,event_id,provider,merchant,reference,amount,currency,status,paid_at) values (?,?,?,?,?,?,?,?,?,?,?)",transaction,mapped?r.attemptId():null,mapped?r.orderId():null,mapped?a.get("event_id"):null,r.provider(),r.merchant(),r.reference(),r.amount(),r.currency(),r.status(),r.paidAt()==null?null:Timestamp.from(r.paidAt()));
  else db.update("update payment.transactions set amount=?,currency=?,status=?,paid_at=? where id=?",r.amount(),r.currency(),r.status(),r.paidAt()==null?null:Timestamp.from(r.paidAt()),transaction);
  if(!mapped){if(r.status().equals("SUCCESS")){openCase(transaction,null,null,"UNMATCHED_TRANSACTION",null,r.amount(),r.currency());financial.changed(null);}return;}
  if(r.status().equals("SUCCESS"))db.update("update payment.attempts set status='SUCCESS',outcome_at=case when status in ('PENDING','FAILED') then clock_timestamp() else outcome_at end,updated_at=clock_timestamp() where id=?",r.attemptId());
  else db.update("update payment.attempts set status='FAILED',outcome_at=clock_timestamp(),updated_at=clock_timestamp() where id=? and status='PENDING'",r.attemptId());
  // An already successful attempt cannot be downgraded by a later failure.
  if(r.status().equals("FAILED")&&!"PENDING".equals(a.get("status")))return;
  repo.audit(transaction,null,"PAYMENT_"+r.status(),"Verified local provider receipt");
  financial.changed((UUID)a.get("event_id"));
  facts.emit(r.orderId(),r.status().equals("SUCCESS")?"succeeded":"failed",Map.of("paymentId",transaction,"attemptId",r.attemptId(),"orderId",r.orderId(),"amount",r.amount(),"currency",r.currency(),"status",r.status()));
 }
 public void openCase(UUID payment,UUID order,UUID event,String type,Long expected,long received,String currency){
  UUID id=UUID.randomUUID();int inserted=db.update("insert into payment.exceptions(id,transaction_id,order_id,event_id,type,expected_amount,received_amount,currency) values (?,?,?,?,?,?,?,?) on conflict(transaction_id) do nothing",id,payment,order,event,type,expected,received,currency);
  if(inserted>0){repo.audit(id,null,"EXCEPTION_OPENED",type);facts.emit(order==null?payment:order,"exception-opened",Map.of("exceptionId",id,"paymentId",payment,"type",type));}
 }
}
