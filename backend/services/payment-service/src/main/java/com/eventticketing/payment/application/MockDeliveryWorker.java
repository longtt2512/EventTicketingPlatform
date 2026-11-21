package com.eventticketing.payment.application;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.mock.enabled",havingValue="true")
public class MockDeliveryWorker {
 private final JdbcTemplate db;private final MockProvider mock;
 public MockDeliveryWorker(JdbcTemplate db,MockProvider mock){this.db=db;this.mock=mock;}
 @Scheduled(fixedDelayString="${app.mock-delivery-delay-ms:1000}")public void run(){for(UUID id:db.queryForList("select attempt_id from payment.mock_sessions where delivered_at is null and available_at<=clock_timestamp() and scenario<>'LOST_CALLBACK' order by available_at limit 50",UUID.class))mock.deliver(id,false);}
}
