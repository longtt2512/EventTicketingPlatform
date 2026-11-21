package com.eventticketing.admission;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.eventticketing.admission.infrastructure.TicketOutboxPublisher;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
@Testcontainers
class TicketOutboxIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 @Test @SuppressWarnings("unchecked") void failedPublishRetainsObligationAndRetryUsesSameMessageIdentity(){
  var source=new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());var db=new JdbcTemplate(source);
  db.execute("create schema ticket_admission");db.execute("create table ticket_admission.outbox(id uuid primary key,aggregate_id uuid,topic text,body jsonb,created_at timestamptz default now(),published_at timestamptz,attempts integer default 0,available_at timestamptz default now())");
  UUID id=UUID.randomUUID(),aggregate=UUID.randomUUID();db.update("insert into ticket_admission.outbox(id,aggregate_id,topic,body) values (?,?,?,?::jsonb)",id,aggregate,"ticket.ticket.issued.v1","{}");
  KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker unavailable")));
  var worker=new TicketOutboxPublisher(db,kafka,new DataSourceTransactionManager(source));worker.publish();
  assertThat(db.queryForObject("select published_at from ticket_admission.outbox where id=?",java.sql.Timestamp.class,id)).isNull();assertThat(db.queryForObject("select attempts from ticket_admission.outbox where id=?",Integer.class,id)).isEqualTo(1);
  db.update("update ticket_admission.outbox set available_at=now()-interval '1 second' where id=?",id);when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));worker.publish();
  assertThat(db.queryForObject("select published_at from ticket_admission.outbox where id=?",java.sql.Timestamp.class,id)).isNotNull();worker.publish();
  var messages=org.mockito.ArgumentCaptor.forClass(ProducerRecord.class);verify(kafka,times(2)).send(messages.capture());
  for(var message:messages.getAllValues()){assertThat(message.key()).isEqualTo(aggregate.toString());assertThat(new String(message.headers().lastHeader("idempotencyKey").value(),java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(id.toString());}
 }
}
