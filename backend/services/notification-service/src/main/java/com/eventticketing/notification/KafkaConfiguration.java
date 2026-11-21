package com.eventticketing.notification;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
public class KafkaConfiguration {
 @Bean DefaultErrorHandler errorHandler(KafkaTemplate<String,String> template){
  var recoverer=new DeadLetterPublishingRecoverer(template,(record,ex)->new TopicPartition(record.topic()+".dlt",record.partition()));
  recoverer.setFailIfSendResultIsError(true);
  var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000,3));handler.setCommitRecovered(true);return handler;
 }
 @Bean KafkaAdmin.NewTopics topics(){return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("event.order.confirmed.v1","ticket.order.ready.v1","ticket.order.cancelled.v1").flatMap(t->java.util.stream.Stream.of(t,t+".dlt")).map(t->TopicBuilder.name(t).partitions(1).replicas(1).build()).toArray(org.apache.kafka.clients.admin.NewTopic[]::new));}
}
