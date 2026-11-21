package com.eventticketing.finance.infrastructure;

import com.eventticketing.finance.application.FinanceProjection;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component @ConditionalOnProperty(name="app.messaging.enabled",havingValue="true",matchIfMissing=true)
public class FinanceListener {
    private final FinanceProjection projection;
    public FinanceListener(FinanceProjection projection){this.projection=projection;}
    @KafkaListener(topics={"event.order.confirmed.v1","payment.payment.financial-updated.v1"})
    public void accept(ConsumerRecord<String,String> record,Acknowledgment ack)throws Exception{projection.accept(record.topic(),record.value());ack.acknowledge();}
}
