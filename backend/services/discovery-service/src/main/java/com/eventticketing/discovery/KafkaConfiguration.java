package com.eventticketing.discovery;
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
 @Bean KafkaAdmin.NewTopics reportTopics(){return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("event.order.confirmed.v1","payment.payment.financial-updated.v1","finance.settlement.updated.v1","finance.settlement.completed.v1").flatMap(t->java.util.stream.Stream.of(t,t+".dlt")).map(t->TopicBuilder.name(t).partitions(1).replicas(1).build()).toArray(org.apache.kafka.clients.admin.NewTopic[]::new));}
 @Bean KafkaAdmin.NewTopics attendeeTopics(){return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("ticket.ticket.issued.v1","ticket.ticket.voided.v1","ticket.admission.checked-in.v1").flatMap(topic->java.util.stream.Stream.of(topic,topic+".dlt")).map(topic->TopicBuilder.name(topic).partitions(1).replicas(1).build()).toArray(org.apache.kafka.clients.admin.NewTopic[]::new));}
 @Bean KafkaAdmin.NewTopics deadLetterTopics(){return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("published","updated","approved","cancelled").map(type->TopicBuilder.name("event.event."+type+".v1.dlt").partitions(1).replicas(1).build()).toArray(org.apache.kafka.clients.admin.NewTopic[]::new));}
}
