package com.eventticketing.notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketNotification {
 private final JdbcTemplate db;private final ObjectMapper json;
 public TicketNotification(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 @Transactional public void accept(String body)throws Exception{var e=json.readTree(body);String type=e.path("eventType").asText();if(e.path("eventVersion").asInt()!=1||!"ticket-admission".equals(e.path("producer").asText())||!Set.of("ticket.order.ready.v1","ticket.order.cancelled.v1").contains(type))throw new IllegalArgumentException("Unsupported ticket notification");var p=e.required("payload");UUID message=UUID.fromString(e.required("eventId").asText()),order=UUID.fromString(p.required("orderId").asText()),customer=UUID.fromString(p.required("customerId").asText());if(db.update("insert into notification.inbox(id) values (?) on conflict do nothing",message)==0)return;db.update("insert into notification.ticket_deliveries(order_id,kind,customer_id,event_name) values (?,?,?,?) on conflict do nothing",order,type.contains("ready")?"READY":"CANCELLED",customer,p.path("eventName").asText("Sự kiện của bạn"));}
}
@Service
class TicketNotificationListener {
 private final TicketNotification notifications;TicketNotificationListener(TicketNotification notifications){this.notifications=notifications;}
 @KafkaListener(topics={"ticket.order.ready.v1","ticket.order.cancelled.v1"},groupId="ticket-notification-v1",autoStartup="${app.messaging.enabled:true}") public void listen(String body,Acknowledgment ack)throws Exception{notifications.accept(body);ack.acknowledge();}
}
