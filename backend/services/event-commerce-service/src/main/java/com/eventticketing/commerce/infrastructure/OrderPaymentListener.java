package com.eventticketing.commerce.infrastructure;
import com.eventticketing.commerce.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
@Component
public class OrderPaymentListener {
 private final ObjectMapper json;private final OrderService orders; public OrderPaymentListener(ObjectMapper json,OrderService orders){this.json=json;this.orders=orders;}
 @KafkaListener(topics={"payment.payment.succeeded.v1","payment.payment.failed.v1","payment.payment.refunded.v1"},autoStartup="${app.messaging.enabled:true}")
 public void receive(ConsumerRecord<String,String> record,Acknowledgment ack)throws Exception{var message=json.readTree(record.value());if(message.path("eventVersion").asInt()!=1)throw new IllegalArgumentException("Unsupported contract version");orders.apply(UUID.fromString(message.path("eventId").asText()),json.treeToValue(message.path("payload"),OrderModels.Evidence.class));ack.acknowledge();}
}
