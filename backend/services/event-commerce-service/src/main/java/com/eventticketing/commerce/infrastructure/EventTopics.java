package com.eventticketing.commerce.infrastructure;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
@Configuration
public class EventTopics {
 @Bean KafkaAdmin.NewTopics topics(){
  return new KafkaAdmin.NewTopics(
   TopicBuilder.name("event.event.published.v1").partitions(1).replicas(1).build(),
   TopicBuilder.name("event.event.updated.v1").partitions(1).replicas(1).build(),
   TopicBuilder.name("event.event.approved.v1").partitions(1).replicas(1).build(),
   TopicBuilder.name("event.event.cancelled.v1").partitions(1).replicas(1).build());
 }
}
