package com.eventticketing.admission.infrastructure;
import com.eventticketing.admission.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
@Component
public class TicketListener {
 private final IssuanceService issuance;private final EventGates gates;private final ObjectMapper json;
 public TicketListener(IssuanceService issuance,EventGates gates,ObjectMapper json){this.issuance=issuance;this.gates=gates;this.json=json;}
 @KafkaListener(topics="event.order.confirmed.v1",autoStartup="${app.messaging.enabled:true}") public void confirmed(String body,Acknowledgment ack){issuance.accept(body);ack.acknowledge();}
 @KafkaListener(topics="event.event.cancelled.v1",autoStartup="${app.messaging.enabled:true}") public void cancelled(String body,Acknowledgment ack)throws Exception{var e=json.readTree(body);if(e.path("eventVersion").asInt()!=1||!"event-commerce".equals(e.path("producer").asText())||!"CANCELLED".equals(e.path("payload").path("status").asText()))throw new IllegalArgumentException("Invalid cancellation fact");gates.freeze(UUID.fromString(e.required("payload").required("id").asText()));ack.acknowledge();}
}
