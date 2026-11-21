package com.eventticketing.commerce.infrastructure;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
public class OrderMessaging {
 @Bean DefaultErrorHandler orderErrorHandler(KafkaTemplate<String,String> template){var recoverer=new DeadLetterPublishingRecoverer(template,(r,e)->new TopicPartition(r.topic()+".dlt",r.partition()));recoverer.setFailIfSendResultIsError(true);var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000,3));handler.setCommitRecovered(true);return handler;}
 @Bean KafkaAdmin.NewTopics orderTopics(){return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("payment.payment.succeeded.v1","payment.payment.failed.v1","payment.payment.refunded.v1","event.order.created.v1","event.order.confirmed.v1","event.order.cancelled.v1","event.order.expired.v1","event.order.payment-result.v1").distinct().flatMap(t->java.util.stream.Stream.of(t,t+".dlt")).map(t->TopicBuilder.name(t).partitions(1).replicas(1).build()).toArray(org.apache.kafka.clients.admin.NewTopic[]::new));}
}
