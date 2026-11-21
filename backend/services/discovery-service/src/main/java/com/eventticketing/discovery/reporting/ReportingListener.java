package com.eventticketing.discovery.reporting;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ReportingListener {
    private final ReportingProjection projection;
    public ReportingListener(ReportingProjection projection){this.projection=projection;}
    @KafkaListener(topics={"event.event.published.v1","event.event.updated.v1","event.event.approved.v1","event.event.cancelled.v1","event.order.confirmed.v1","payment.payment.financial-updated.v1","finance.settlement.updated.v1","finance.settlement.completed.v1"},groupId="operations-reporting-v1",autoStartup="${app.messaging.enabled:true}")
    public void accept(ConsumerRecord<String,String> record,Acknowledgment ack)throws Exception{projection.accept(record.topic(),record.value());ack.acknowledge();}
}
