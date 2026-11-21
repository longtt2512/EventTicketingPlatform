package com.eventticketing.notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OrderNotification {
 private final JdbcTemplate db;private final ObjectMapper json;
 public OrderNotification(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 @Transactional public void accept(String body)throws Exception{var envelope=json.readTree(body);if(envelope.path("eventVersion").asInt()!=1)throw new IllegalArgumentException("Unsupported order notification version");UUID id=UUID.fromString(envelope.path("eventId").asText());var p=envelope.path("payload");UUID order=UUID.fromString(p.path("id").asText()),customer=UUID.fromString(p.path("customerId").asText());if(!p.path("status").asText().equals("CONFIRMED")||!p.path("totalVnd").canConvertToLong()||p.path("totalVnd").asLong()<0)throw new IllegalArgumentException("Only confirmed orders are eligible");if(db.update("insert into notification.inbox(id) values (?) on conflict do nothing",id)==0)return;db.update("insert into notification.email_deliveries(order_id,customer_id,event_name,total_vnd) values (?,?,?,?) on conflict(order_id) do nothing",order,customer,p.path("eventName").asText(),p.path("totalVnd").asLong());}
}
