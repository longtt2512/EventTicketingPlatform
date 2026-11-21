package com.eventticketing.payment.infrastructure;
import com.eventticketing.payment.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
@Component
public class PaymentOrderListener {
 private final ObjectMapper json;private final PaymentReconciliation reconciliation; public PaymentOrderListener(ObjectMapper json,PaymentReconciliation reconciliation){this.json=json;this.reconciliation=reconciliation;}
 @KafkaListener(topics={"event.order.payment-result.v1","event.event.cancelled.v1"},autoStartup="${app.messaging.enabled:true}")
 public void receive(ConsumerRecord<String,String> record,Acknowledgment ack)throws Exception{var message=json.readTree(record.value());if(message.path("eventVersion").asInt()!=1)throw new IllegalArgumentException("Unsupported contract version");reconciliation.apply(UUID.fromString(message.path("eventId").asText()),record.topic(),message.path("payload"));ack.acknowledge();}
}
