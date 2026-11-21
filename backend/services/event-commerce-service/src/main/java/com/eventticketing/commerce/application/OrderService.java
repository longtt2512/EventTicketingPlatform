package com.eventticketing.commerce.application;

import static com.eventticketing.commerce.application.OrderModels.*;
import static com.eventticketing.commerce.application.OrderRepository.*;
import com.eventticketing.commerce.api.ApiException;
import com.eventticketing.commerce.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
 private final JdbcTemplate db;private final OrderRepository orders;private final EventRepository events;
 private final InventoryService inventory;private final OrderFacts facts;private final IdentityClient identity;
 private final OrganizationScopeClient scope;private final ObjectMapper json;private final CommissionClient commission;
 public OrderService(JdbcTemplate db,OrderRepository orders,EventRepository events,InventoryService inventory,OrderFacts facts,IdentityClient identity,OrganizationScopeClient scope,ObjectMapper json,CommissionClient commission){this.commission=commission;this.db=db;this.orders=orders;this.events=events;this.inventory=inventory;this.facts=facts;this.identity=identity;this.scope=scope;this.json=json;}
 public Quote quote(UUID eventId){var e=events.get(eventId);if(!e.status().equals("PUBLISHED"))throw conflict("EVENT_UNAVAILABLE","Sự kiện hiện không mở bán. Hãy chọn sự kiện khác.");scope.active(e.organizationId());return new Quote(e,selectionVersion(e),Instant.now());}
 private String selectionVersion(EventModels.EventView e){
  var values=new ArrayList<Object>();values.add(e.id());values.add(e.terms());values.add(e.startAt());values.add(e.endAt());values.add(e.status());
  e.ticketTypes().forEach(t->values.add(List.of(t.id(),t.version(),t.price())));
  try{return hash(json.writeValueAsString(values));}catch(Exception ex){throw new IllegalStateException(ex);}
 }
 @Transactional public Order reserve(UUID actor,String key,Reserve input){
  eligible(actor);key(key);var quantities=new TreeMap<UUID,Integer>();
  for(var line:input.lines()){try{quantities.merge(line.ticketTypeId(),line.quantity(),Math::addExact);}catch(ArithmeticException ex){throw conflict("LIMIT_EXCEEDED","Số lượng vé vượt giới hạn.");}}
  String fingerprint=hash("reserve|"+input.eventId()+"|"+quantities+"|"+input.selectionVersion()+"|"+input.termsAccepted());
  UUID replay=request(actor,key,fingerprint);if(replay!=null)return orders.get(replay);
  var e=events.lock(input.eventId());
  if(!input.termsAccepted())throw conflict("TERMS_REQUIRED","Vui lòng đồng ý điều khoản tham dự.");
  if(!selectionVersion(e).equals(input.selectionVersion()))throw conflict("PRICE_CHANGED","Giá vé hoặc điều khoản đã thay đổi. Tải lại và xác nhận lựa chọn mới.");
  if(quantities.isEmpty())throw conflict("TICKETS_REQUIRED","Hãy chọn ít nhất một vé.");
  long total=0;
  for(var line:quantities.entrySet()){
   var t=e.ticketTypes().stream().filter(x->x.id().equals(line.getKey())).findFirst().orElseThrow(OrderRepository::missing);
   if(line.getValue()<=0||line.getValue()>t.maxPerOrder())throw conflict("LIMIT_EXCEEDED","Số lượng vượt giới hạn mỗi đơn. Hãy giảm số vé.");
   if(!t.saleState().equals("ON_SALE")||line.getValue()>t.available())throw conflict("INSUFFICIENT_INVENTORY","Hạng vé không còn đủ số lượng hoặc đã ngừng bán. Hãy chọn lại.");
   try{total=Math.addExact(total,Math.multiplyExact(t.price(),line.getValue()));}catch(ArithmeticException ex){throw conflict("LIMIT_EXCEEDED","Tổng tiền vượt giới hạn xử lý.");}
  }
  UUID id=UUID.randomUUID();
  var feeLines=new ArrayList<Map<String,Object>>();
  for(var t:e.ticketTypes())if(quantities.containsKey(t.id()))feeLines.add(Map.of("ticketTypeId",t.id(),"unitPrice",t.price(),"quantity",quantities.get(t.id())));
  var snapshot=commission.quote(id,feeLines);
  if(snapshot==null||snapshot.path("grossVnd").asLong(-1)!=total)throw conflict("COMMISSION_UNAVAILABLE","Snapshot phí không khớp đơn; hãy thử lại.");
  Instant captured=Instant.parse(snapshot.path("capturedAt").asText()),expires=captured.plusSeconds(600);
  if(!Instant.now().isBefore(expires))throw conflict("COMMISSION_UNAVAILABLE","Snapshot phí đã hết hạn; hãy thử lại.");
  inventory.reserve(e.id(),id,quantities,expires);
  db.update("insert into event_commerce.orders(id,customer_id,event_id,organization_id,event_name,terms_snapshot,status,total_vnd,expires_at,created_at,commission_snapshot) values (?,?,?,?,?,?,'PENDING_PAYMENT',?,?,?,?::jsonb)",id,actor,e.id(),e.organizationId(),e.name(),e.terms(),total,Timestamp.from(expires),Timestamp.from(captured),snapshot.toString());
  for(var t:e.ticketTypes())if(quantities.containsKey(t.id()))db.update("insert into event_commerce.order_items(id,order_id,ticket_type_id,name,quantity,unit_price) values (?,?,?,?,?,?)",UUID.randomUUID(),id,t.id(),t.name(),quantities.get(t.id()),t.price());
  saveRequest(actor,key,fingerprint,id);facts.audit(id,actor,"RESERVED");facts.emit(id,"created",orders.get(id));return orders.get(id);
 }
 @Transactional public Order read(UUID id,UUID actor,boolean admin){var o=orders.get(id);authorize(o,actor,admin);orders.lock(id);expireLocked(id);return orders.get(id);}
 public Order scopedRead(UUID id,UUID actor,boolean admin){var o=orders.get(id);if(!admin&&!o.customerId().equals(actor))scope.require(o.organizationId(),actor,false);return o;}
 public Page list(UUID actor,String status,String cursor,int limit){return page("customer_id",actor,null,status,cursor,limit);}
 public Page eventOrders(UUID org,UUID event,UUID actor,boolean admin,String status,String cursor,int limit){var e=events.get(event);if(!e.organizationId().equals(org))throw missing();if(!admin)scope.require(org,actor,false);return page("event_id",event,org,status,cursor,limit);}
 private Page page(String column,UUID value,UUID org,String status,String cursor,int limit){
  if(limit<1||limit>100)throw conflict("INVALID_PAGE","Giới hạn trang từ 1 đến 100.");
  var args=new ArrayList<Object>();args.add(value);String filter="";
  if(status!=null&&!status.isBlank()){if(!Set.of("PENDING_PAYMENT","CONFIRMED","CANCELLED","EXPIRED").contains(status))throw conflict("INVALID_STATUS","Trạng thái đơn hàng không hợp lệ.");filter+=" and status=?";args.add(status);}
  if(cursor!=null&&!cursor.isBlank()){try{String[] parts=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8).split("\\|",-1);filter+=" and (created_at,id)<(?,?)";args.add(Timestamp.from(Instant.parse(parts[0])));args.add(UUID.fromString(parts[1]));}catch(Exception ex){throw conflict("INVALID_CURSOR","Trang đã hết hiệu lực. Tải lại danh sách.");}}
  args.add(limit+1);var ids=db.queryForList("select id from event_commerce.orders where "+column+"=?"+filter+" order by created_at desc,id desc limit ?",UUID.class,args.toArray());boolean more=ids.size()>limit;var data=ids.stream().limit(limit).map(orders::get).toList();
  String next=more?Base64.getUrlEncoder().withoutPadding().encodeToString((data.getLast().createdAt()+"|"+data.getLast().id()).getBytes(StandardCharsets.UTF_8)):null;return new Page(data,more,next);
 }
 @Transactional public Order command(UUID id,UUID actor,String key,String command){
  key(key);String fp=hash(command+"|"+id);UUID prior=request(actor,key,fp);if(prior!=null)return orders.get(prior);
  var o=orders.lock(id);authorize(o,actor,false);expireLocked(id);o=orders.get(id);
  if(command.equals("confirm-free")){
   eligible(actor);
   if(!o.canConfirmFree())throw conflict("ORDER_NOT_ELIGIBLE","Đơn không đủ điều kiện xác nhận miễn phí. Hãy kiểm tra trạng thái.");
   inventory.close(o.eventId(),id,"CONSUMED");confirm(id,actor,null,"FREE");
  }else{
   if(!o.canCancel())throw conflict("PAYMENT_UNRESOLVED","Chỉ hủy được đơn còn hạn và không có thanh toán đang chờ. Hãy kiểm tra trạng thái thanh toán.");
   inventory.close(o.eventId(),id,"RELEASED");db.update("update event_commerce.orders set status='CANCELLED',updated_at=clock_timestamp(),version=version+1 where id=?",id);facts.audit(id,actor,"CANCELLED");facts.emit(id,"cancelled",orders.get(id));
  }
  saveRequest(actor,key,fp,id);return orders.get(id);
 }
 @Transactional public PaymentClaim claim(UUID id,Claim input){
  eligible(input.customerId());key(input.requestKey());var o=orders.lock(id);authorize(o,input.customerId(),false);expireLocked(id);
  var prior=db.queryForList("select id from event_commerce.order_payment_claims where order_id=? and request_key=?",UUID.class,id,input.requestKey());
  if(!prior.isEmpty())return new PaymentClaim(prior.getFirst(),orders.get(id));
  o=orders.get(id);if(!o.canPay())throw conflict("PAYMENT_UNRESOLVED","Không thể tạo thanh toán mới. Kiểm tra giao dịch đang chờ hoặc thời hạn đơn.");
  UUID attempt=UUID.randomUUID();db.update("insert into event_commerce.order_payment_claims(id,order_id,request_key) values (?,?,?)",attempt,id,input.requestKey());
  db.update("update event_commerce.orders set active_attempt_id=?,payment_status='PENDING',version=version+1,updated_at=clock_timestamp() where id=?",attempt,id);facts.audit(id,input.customerId(),"PAYMENT_STARTED");return new PaymentClaim(attempt,orders.get(id));
 }
 @Transactional public void apply(UUID messageId,Evidence p){
  var o=orders.lock(p.orderId());
  if(db.update("insert into event_commerce.order_inbox(id) values (?) on conflict do nothing",messageId)==0)return;
  if(db.queryForObject("select count(*) from event_commerce.order_payment_claims where id=? and order_id=?",Integer.class,p.attemptId(),p.orderId())!=1)throw new IllegalArgumentException("Unknown payment claim");
  expireLocked(o.id());o=orders.get(o.id());
  if(p.status().equals("FAILED")){
   db.update("update event_commerce.order_payment_claims set status='FAILED' where id=? and status='PENDING'",p.attemptId());
   db.update("update event_commerce.orders set payment_status='FAILED',updated_at=clock_timestamp(),version=version+1 where id=? and active_attempt_id=? and payment_status='PENDING'",o.id(),p.attemptId());return;
  }
  if(p.status().equals("REFUNDED")){
   db.update("update event_commerce.order_payment_effects set result='REFUNDED' where payment_id=? and order_id=?",p.paymentId(),o.id());
   int unresolved=db.queryForObject("select count(*) from event_commerce.order_payment_effects where order_id=? and result not in ('APPLIED','REFUNDED')",Integer.class,o.id());
   boolean appliedExists=db.queryForObject("select count(*) from event_commerce.order_payment_effects where order_id=? and result='APPLIED'",Integer.class,o.id())>0;
   boolean pending=db.queryForObject("select count(*) from event_commerce.order_payment_claims where order_id=? and status='PENDING'",Integer.class,o.id())>0;
   String state=unresolved>0?"EXCEPTION":o.status().equals("CONFIRMED")&&appliedExists?"SUCCESS":pending?"PENDING":"REFUNDED";
   db.update("update event_commerce.orders set payment_status=?,version=version+1,updated_at=clock_timestamp() where id=?",state,o.id());return;
  }
  if(!p.status().equals("SUCCESS"))throw new IllegalArgumentException("Unsupported payment state");
  var prior=db.queryForList("select result from event_commerce.order_payment_effects where payment_id=?",String.class,p.paymentId());if(!prior.isEmpty())return;
  String result=!p.currency().equals(o.currency())||p.amount()!=o.totalVnd()?"AMOUNT_MISMATCH":o.eventStatus().equals("CANCELLED")?"EVENT_CANCELLED":o.status().equals("CONFIRMED")?"DUPLICATE_COLLECTION":!o.status().equals("PENDING_PAYMENT")||!Instant.now().isBefore(o.expiresAt())?"LATE_SUCCESS":!o.eventStatus().equals("PUBLISHED")?"EVENT_UNAVAILABLE":"APPLIED";
  db.update("update event_commerce.order_payment_claims set status='SUCCESS' where id=?",p.attemptId());
  if(result.equals("APPLIED")){inventory.close(o.eventId(),o.id(),"CONSUMED");confirm(o.id(),null,p.paymentId(),"SUCCESS");}
  else {db.update("update event_commerce.orders set payment_status='EXCEPTION',updated_at=clock_timestamp(),version=version+1 where id=?",o.id());facts.audit(o.id(),null,result);}
  db.update("insert into event_commerce.order_payment_effects(payment_id,order_id,result) values (?,?,?)",p.paymentId(),o.id(),result);
  facts.emit(o.id(),"payment-result",Map.of("paymentId",p.paymentId(),"attemptId",p.attemptId(),"orderId",o.id(),"eventId",o.eventId(),"result",result,"expectedAmount",o.totalVnd(),"receivedAmount",p.amount(),"currency",p.currency()));
 }
 private void confirm(UUID id,UUID actor,UUID payment,String state){db.update("update event_commerce.orders set status='CONFIRMED',payment_status=?,applied_payment_id=?,confirmed_at=clock_timestamp(),updated_at=clock_timestamp(),version=version+1 where id=?",state,payment,id);facts.audit(id,actor,"CONFIRMED");facts.emit(id,"confirmed",orders.get(id));}
 @Transactional public void expire(UUID id){orders.lock(id);expireLocked(id);}
 private void expireLocked(UUID id){var o=orders.get(id);if(!o.status().equals("PENDING_PAYMENT"))return;
  boolean cancelled=o.eventStatus().equals("CANCELLED");if(!cancelled&&Instant.now().isBefore(o.expiresAt()))return;
  var state=db.queryForObject("select state from event_commerce.inventory_allocations where id=?",String.class,id);
  if("ACTIVE".equals(state))inventory.close(o.eventId(),id,cancelled?"RELEASED":"EXPIRED");String target=cancelled?"CANCELLED":"EXPIRED";
  db.update("update event_commerce.orders set status=?,updated_at=clock_timestamp(),version=version+1 where id=?",target,id);facts.audit(id,null,target);facts.emit(id,target.toLowerCase(Locale.ROOT),orders.get(id));
 }
 private void authorize(Order o,UUID actor,boolean admin){if(!admin&&!o.customerId().equals(actor))throw missing();}
 private void eligible(UUID actor){var s=identity.securityState(actor);if(s==null||!"ACTIVE".equals(s.status())||!s.emailVerified()||!s.roles().contains("CUSTOMER"))throw new ApiException(HttpStatus.FORBIDDEN,"VERIFIED_CUSTOMER_REQUIRED","Bạn cần xác thực email trước khi đặt vé. Mở trang xác thực email để gửi lại thư.");}
 private UUID request(UUID actor,String key,String fp){db.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))",actor+"|"+key);var r=db.queryForList("select request_hash,order_id from event_commerce.order_requests where actor_id=? and request_key=?",actor,key);if(r.isEmpty())return null;if(!fp.equals(r.getFirst().get("request_hash")))throw conflict("IDEMPOTENCY_CONFLICT","Yêu cầu đã được dùng với nội dung khác. Hãy kiểm tra đơn hàng trước khi thử lại.");return (UUID)r.getFirst().get("order_id");}
 private void saveRequest(UUID actor,String key,String fp,UUID id){db.update("insert into event_commerce.order_requests(actor_id,request_key,request_hash,order_id) values (?,?,?,?)",actor,key,fp,id);}
 public static void key(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{8,128}"))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_REQUEST_KEY","Mã yêu cầu không hợp lệ. Tải lại trang để thử lại.");}
 private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
