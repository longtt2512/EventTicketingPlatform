package com.eventticketing.discovery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
@Component
public class ProjectionListener {
 private final ProjectionService projections;public ProjectionListener(ProjectionService projections){this.projections=projections;}
 @KafkaListener(topics={"event.event.published.v1","event.event.updated.v1","event.event.approved.v1","event.event.cancelled.v1"},autoStartup="${app.messaging.enabled:true}")
 public void receive(String body,Acknowledgment ack){projections.apply(body);ack.acknowledge();}
}
