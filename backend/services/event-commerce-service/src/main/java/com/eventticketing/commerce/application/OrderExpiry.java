package com.eventticketing.commerce.application;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class OrderExpiry {
 private final JdbcTemplate db;private final OrderService orders;
 public OrderExpiry(JdbcTemplate db,OrderService orders){this.db=db;this.orders=orders;}
 @Scheduled(fixedDelayString="${app.order-expiry-delay-ms:3000}") public void run(){
  for(UUID id:db.queryForList("select o.id from event_commerce.orders o join event_commerce.events e on e.id=o.event_id where o.status='PENDING_PAYMENT' and (o.expires_at<=clock_timestamp() or e.status='CANCELLED') order by o.event_id,o.id limit 100",UUID.class))orders.expire(id);
 }
}
