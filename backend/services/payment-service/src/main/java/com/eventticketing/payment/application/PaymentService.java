package com.eventticketing.payment.application;

import static com.eventticketing.payment.application.PaymentModels.*;
import static com.eventticketing.payment.application.PaymentRepository.*;
import com.eventticketing.payment.infrastructure.CommerceClient;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
 private final FinancialLedger ledger;private final JdbcTemplate db;private final CommerceClient commerce;private final PaymentRepository repo;private final MockProperties config;
 public PaymentService(JdbcTemplate db,CommerceClient commerce,PaymentRepository repo,MockProperties config,FinancialLedger ledger){this.ledger=ledger;this.db=db;this.commerce=commerce;this.repo=repo;this.config=config;}
 @Transactional public Attempt start(UUID order,UUID actor,String key){
  if(!config.enabled())throw invalid("Thanh toán đang tạm thời gián đoạn. Vui lòng thử lại sau.");
  if(key==null||!key.matches("[A-Za-z0-9_-]{8,128}"))throw invalid("Mã yêu cầu không hợp lệ. Tải lại trang.");
  var snapshot=commerce.read(order);if(!snapshot.customerId().equals(actor))throw missing();
  var claim=commerce.claim(order,actor,key);var o=claim.order();UUID id=claim.attemptId();ledger.lock(o.eventId());
  // Claim identity is durable in Commerce: retry after a response/process failure recovers the same attempt.
  int inserted=db.update("insert into payment.attempts(id,order_id,customer_id,event_id,organization_id,expected_amount,currency,expires_at) values (?,?,?,?,?,?,?,?) on conflict(id) do nothing",id,order,actor,o.eventId(),o.organizationId(),o.totalVnd(),o.currency(),Timestamp.from(o.expiresAt()));
  db.update("insert into payment.mock_sessions(attempt_id,reference,received_amount) values (?,?,?) on conflict(attempt_id) do nothing",id,"MOCK-"+id,o.totalVnd());
  if(inserted>0)ledger.changed(o.eventId());
  return repo.get(id);
 }
 public List<Attempt> list(UUID order,UUID actor,boolean admin){var o=commerce.read(order);if(!admin&&!o.customerId().equals(actor))throw missing();return db.queryForList("select id from payment.attempts where order_id=? order by created_at desc,id desc",UUID.class,order).stream().map(repo::get).toList();}
 public Attempt read(UUID id,UUID actor,boolean admin){repo.owner(id,actor,admin);return repo.get(id);}
}
