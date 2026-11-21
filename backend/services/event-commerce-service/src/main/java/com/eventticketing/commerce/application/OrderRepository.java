package com.eventticketing.commerce.application;

import static com.eventticketing.commerce.application.OrderModels.*;
import com.eventticketing.commerce.api.ApiException;
import java.time.Instant;
import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
 private final JdbcTemplate db;private final EventRepository events;
 public OrderRepository(JdbcTemplate db,EventRepository events){this.db=db;this.events=events;}
 public Order get(UUID id){return db.query("select * from event_commerce.orders where id=?",this::map,id).stream().findFirst().orElseThrow(OrderRepository::missing);}
 public Order lock(UUID id){var current=get(id);events.lock(current.eventId());db.queryForList("select id from event_commerce.orders where id=? for update",id);return get(id);}
 public List<Item> items(UUID id){return db.query("select * from event_commerce.order_items where order_id=? order by id",(r,n)->new Item(r.getObject("id",UUID.class),r.getObject("ticket_type_id",UUID.class),r.getString("name"),r.getInt("quantity"),r.getLong("unit_price"),Math.multiplyExact(r.getLong("unit_price"),r.getInt("quantity"))),id);}
 private Order map(ResultSet r,int n)throws SQLException {
  Instant now=Instant.now(),expires=r.getTimestamp("expires_at").toInstant();
  String status=r.getString("status"),payment=r.getString("payment_status"),eventStatus=events.get(r.getObject("event_id",UUID.class)).status();
  boolean pending=events.get(r.getObject("event_id",UUID.class)).cancellationPending();
  boolean active=!pending&&status.equals("PENDING_PAYMENT")&&now.isBefore(expires)&&eventStatus.equals("PUBLISHED");
  boolean safe=Set.of("NONE","FAILED","REFUNDED").contains(payment);long total=r.getLong("total_vnd");
  UUID id=r.getObject("id",UUID.class);Timestamp confirmed=r.getTimestamp("confirmed_at");
  return new Order(id,r.getObject("customer_id",UUID.class),r.getObject("event_id",UUID.class),r.getObject("organization_id",UUID.class),r.getString("event_name"),r.getString("terms_snapshot"),status,eventStatus,total,r.getString("currency"),expires,r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),confirmed==null?null:confirmed.toInstant(),payment,r.getObject("active_attempt_id",UUID.class),items(id),now,active&&safe&&total>0,active&&safe,active&&safe&&total==0,r.getLong("version"));
 }
 public static ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND","Không tìm thấy đơn hàng hoặc bạn không có quyền xem.");}
 public static ApiException conflict(String code,String message){return new ApiException(HttpStatus.CONFLICT,code,message);}
}
