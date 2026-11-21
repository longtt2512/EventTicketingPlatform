package com.eventticketing.finance.infrastructure;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
public class FinanceMessaging {
 @Bean DefaultErrorHandler financeErrorHandler(KafkaTemplate<String,String> template){var recoverer=new DeadLetterPublishingRecoverer(template,(r,e)->new TopicPartition(r.topic()+".dlt",r.partition()));recoverer.setFailIfSendResultIsError(true);var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000,3));handler.setCommitRecovered(true);return handler;}
 @Bean KafkaAdmin.NewTopics financeTopics(){return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("event.order.confirmed.v1","payment.payment.financial-updated.v1","finance.settlement.updated.v1","finance.settlement.completed.v1").distinct().flatMap(t->java.util.stream.Stream.of(t,t+".dlt")).map(t->TopicBuilder.name(t).partitions(1).replicas(1).build()).toArray(org.apache.kafka.clients.admin.NewTopic[]::new));}
}
