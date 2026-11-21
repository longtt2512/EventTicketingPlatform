package com.eventticketing.payment.application;

import static com.eventticketing.payment.application.PaymentModels.*;
import static com.eventticketing.payment.application.PaymentRepository.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAdmin {
 private final FinancialLedger financial;private final JdbcTemplate db;private final PaymentRepository repo;private final PaymentFacts facts;
 public PaymentAdmin(JdbcTemplate db,PaymentRepository repo,PaymentFacts facts,FinancialLedger financial){this.financial=financial;this.db=db;this.repo=repo;this.facts=facts;}
 public Map<String,Object> list(boolean exceptions,String status,UUID order,UUID cursor,int limit){
  if(limit<1||limit>100)throw invalid("Giới hạn trang từ 1 đến 100.");
  String table=exceptions?"exceptions":"attempts",where=" where 1=1";var args=new ArrayList<Object>();
  if(status!=null&&!status.isBlank()){if(!(exceptions?Set.of("OPEN","IN_REVIEW","RESOLVED"):Set.of("PENDING","SUCCESS","FAILED","REFUNDED")).contains(status))throw invalid("Bộ lọc trạng thái không hợp lệ.");where+=" and status=?";args.add(status);}
  if(order!=null){where+=" and order_id=?";args.add(order);}if(cursor!=null){where+=" and id>?";args.add(cursor);}args.add(limit+1);
  var rows=db.queryForList("select * from payment."+table+where+" order by id limit ?",args.toArray());boolean more=rows.size()>limit;var data=rows.stream().limit(limit).map(PaymentAdmin::normalize).toList();var result=new LinkedHashMap<String,Object>();result.put("data",data);result.put("hasMore",more);result.put("nextCursor",more?data.getLast().get("id"):null);return result;
 }
 public Map<String,Object> detail(UUID id){var rows=db.queryForList("select * from payment.exceptions where id=?",id);if(rows.isEmpty())throw missing();var result=normalize(rows.getFirst());result.put("returns",db.queryForList("select * from payment.return_evidence where exception_id=? order by created_at",id).stream().map(PaymentAdmin::normalize).toList());result.put("history",db.queryForList("select action,detail,occurred_at from payment.audit where resource_id=? order by occurred_at,id",id).stream().map(PaymentAdmin::normalize).toList());return result;}
 public List<Map<String,Object>> summaries(UUID order){return db.queryForList("select id,type,status,received_amount,returned_amount,currency from payment.exceptions where order_id=? order by created_at",order).stream().map(PaymentAdmin::normalize).toList();}
 @Transactional public Map<String,Object> review(UUID id,UUID actor,String reason){var r=lock(id);if("RESOLVED".equals(r.get("status")))throw invalid("Ngoại lệ đã được xử lý. Hãy tải lại danh sách.");if(r.get("reviewer_id")!=null&&!actor.equals(r.get("reviewer_id")))throw invalid("Ngoại lệ đang được quản trị viên khác xử lý.");db.update("update payment.exceptions set status='IN_REVIEW',reviewer_id=?,reason=?,updated_at=clock_timestamp() where id=?",actor,reason.strip(),id);repo.audit(id,actor,"REVIEW_STARTED",reason.strip());return detail(id);}
 @Transactional public Map<String,Object> resolve(UUID id,UUID actor,Return input){
  Instant returnedAt=input.returnedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  var r=lock(id);var old=db.queryForList("select * from payment.return_evidence where reference=?",input.reference());
  if(!old.isEmpty()){var e=old.getFirst();if(id.equals(e.get("exception_id"))&&input.amount()==((Number)e.get("amount")).longValue()&&input.currency().equals(e.get("currency"))&&input.evidence().strip().equals(e.get("evidence"))&&returnedAt.equals(((Timestamp)e.get("returned_at")).toInstant()))return detail(id);throw invalid("Mã hoàn đã được dùng với nội dung khác. Kiểm tra bằng chứng trước khi ghi nhận.");}
  if(!"IN_REVIEW".equals(r.get("status"))||!actor.equals(r.get("reviewer_id")))throw invalid("Hãy nhận xử lý ngoại lệ trước khi ghi nhận khoản hoàn.");
  long outstanding=((Number)r.get("received_amount")).longValue()-((Number)r.get("returned_amount")).longValue();
  if(input.amount()<=0||input.amount()>outstanding||!input.currency().equals(r.get("currency"))||input.returnedAt().isAfter(Instant.now())||input.returnedAt().isBefore(((Timestamp)r.get("created_at")).toInstant().truncatedTo(java.time.temporal.ChronoUnit.MINUTES))||!input.reference().matches("MOCK-RETURN-[A-Za-z0-9-]{6,80}")||input.evidence().strip().length()<10)throw invalid("Nhập khoản hoàn đúng tiền tệ, không vượt số tiền còn lại; ngày hoàn từ lúc mở ngoại lệ đến hiện tại, mã MOCK-RETURN-… và bằng chứng ít nhất 10 ký tự.");
  db.update("insert into payment.return_evidence(id,exception_id,reference,amount,currency,evidence,returned_at,actor_id) values (?,?,?,?,?,?,?,?)",UUID.randomUUID(),id,input.reference(),input.amount(),input.currency(),input.evidence().strip(),Timestamp.from(returnedAt),actor);
  boolean resolved=input.amount()==outstanding;
  db.update("update payment.exceptions set returned_amount=returned_amount+?,status=?,updated_at=clock_timestamp() where id=?",input.amount(),resolved?"RESOLVED":"IN_REVIEW",id);repo.audit(id,actor,resolved?"RESOLVED":"RETURN_RECORDED",input.reference()+": "+input.evidence().strip());
  if(resolved){var t=db.queryForList("select * from payment.transactions where id=?",r.get("transaction_id")).getFirst();db.update("update payment.transactions set status='REFUNDED' where id=?",t.get("id"));if(t.get("attempt_id")!=null){db.update("update payment.attempts set status='REFUNDED',updated_at=clock_timestamp() where id=?",t.get("attempt_id"));facts.emit((UUID)t.get("order_id"),"refunded",Map.of("paymentId",t.get("id"),"attemptId",t.get("attempt_id"),"orderId",t.get("order_id"),"amount",t.get("amount"),"currency",t.get("currency"),"status","REFUNDED"));}}
  financial.changed((UUID)r.get("event_id"));return detail(id);
 }
 private Map<String,Object> lock(UUID id){var ids=db.queryForList("select event_id from payment.exceptions where id=?",UUID.class,id);if(!ids.isEmpty())financial.lock(ids.getFirst());var rows=db.queryForList("select * from payment.exceptions where id=? for update",id);if(rows.isEmpty())throw missing();return rows.getFirst();}
 public static Map<String,Object> normalize(Map<String,Object> row){var r=new LinkedHashMap<String,Object>();row.forEach((k,v)->r.put(k,v instanceof Timestamp t?t.toInstant().toString():v));return r;}
}
