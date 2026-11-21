package com.eventticketing.payment.application;
import static com.eventticketing.payment.application.PaymentModels.*;
import com.eventticketing.payment.api.ApiException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class PaymentRepository {
 private final JdbcTemplate db;public PaymentRepository(JdbcTemplate db){this.db=db;}
 public Attempt get(UUID id){return db.query("select a.*,m.scenario,m.available_at,m.payment_method,m.method_expires_at,m.card_last_four,m.failure_code,m.failure_message from payment.attempts a join payment.mock_sessions m on a.id=m.attempt_id where a.id=?",(r,n)->new Attempt(id,r.getObject("order_id",UUID.class),r.getObject("event_id",UUID.class),r.getLong("expected_amount"),r.getString("currency"),r.getString("status"),r.getString("result"),r.getTimestamp("expires_at").toInstant(),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),"local-mock","/payment/checkout/"+id,r.getString("scenario"),r.getTimestamp("available_at")==null?null:r.getTimestamp("available_at").toInstant(),r.getString("payment_method"),r.getTimestamp("method_expires_at")==null?null:r.getTimestamp("method_expires_at").toInstant(),r.getString("card_last_four"),r.getString("failure_code"),r.getString("failure_message")),id).stream().findFirst().orElseThrow(PaymentRepository::missing);}
 public void owner(UUID id,UUID actor,boolean admin){var owners=db.queryForList("select customer_id from payment.attempts where id=?",UUID.class,id);if(owners.isEmpty()||(!admin&&!owners.getFirst().equals(actor)))throw missing();}
 public void audit(UUID id,UUID actor,String action,String detail){db.update("insert into payment.audit(id,resource_id,actor_id,action,detail) values (?,?,?,?,?)",UUID.randomUUID(),id,actor,action,detail);}
 public static ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"PAYMENT_NOT_FOUND","Không tìm thấy giao dịch hoặc bạn không có quyền xem.");}
 public static ApiException invalid(String text){return new ApiException(HttpStatus.CONFLICT,"PAYMENT_VALIDATION",text);}
}
