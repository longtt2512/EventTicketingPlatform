package com.eventticketing.notification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
@Component
public class OrderListener {
 private final OrderNotification notifications;public OrderListener(OrderNotification notifications){this.notifications=notifications;}
 @KafkaListener(topics="event.order.confirmed.v1",autoStartup="${app.messaging.enabled:true}")public void receive(String body,Acknowledgment ack)throws Exception{notifications.accept(body);ack.acknowledge();}
}
