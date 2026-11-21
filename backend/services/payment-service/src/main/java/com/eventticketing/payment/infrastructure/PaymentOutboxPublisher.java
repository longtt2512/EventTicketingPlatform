package com.eventticketing.payment.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name="app.messaging.enabled",havingValue="true",matchIfMissing=true)
public class PaymentOutboxPublisher {
    private final JdbcTemplate db;private final KafkaTemplate<String,String> kafka;private final TransactionTemplate transaction;
    public PaymentOutboxPublisher(JdbcTemplate db,KafkaTemplate<String,String> kafka,PlatformTransactionManager manager){this.db=db;this.kafka=kafka;transaction=new TransactionTemplate(manager);}
    @Scheduled(fixedDelayString="${app.outbox-delay-ms:1000}") public void publish() {
        transaction.executeWithoutResult(tx->{
            var rows=db.queryForList("select id,aggregate_id,topic,body::text as body from payment.outbox where published_at is null and available_at<=clock_timestamp() order by created_at limit 10 for update skip locked");
            for(var row:rows) {
                var message=new ProducerRecord<String,String>((String)row.get("topic"),row.get("aggregate_id").toString(),(String)row.get("body"));
                String id=row.get("id").toString();
                for(String key:new String[]{"correlationId","idempotencyKey"})message.headers().add(key,id.getBytes(StandardCharsets.UTF_8));
                message.headers().add("eventVersion","1".getBytes(StandardCharsets.UTF_8));
                message.headers().add("eventType",row.get("topic").toString().getBytes(StandardCharsets.UTF_8));
                try {
                    kafka.send(message).get(5,TimeUnit.SECONDS);
                    db.update("update payment.outbox set published_at=clock_timestamp(),attempts=attempts+1 where id=?",row.get("id"));
                } catch(Exception ex) {
                    if(ex instanceof InterruptedException)Thread.currentThread().interrupt();
                    db.update("update payment.outbox set attempts=attempts+1,available_at=clock_timestamp()+interval '10 seconds' where id=?",row.get("id"));
                    org.slf4j.LoggerFactory.getLogger(getClass()).warn("Event outbox delivery deferred: {}",id);
                }
            }
        });
    }
}
