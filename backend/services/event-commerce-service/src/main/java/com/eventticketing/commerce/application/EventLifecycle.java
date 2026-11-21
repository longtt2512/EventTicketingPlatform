package com.eventticketing.commerce.application;

import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class EventLifecycle {
    private final EventRepository repo;private final EventChanges changes;private final TransactionTemplate transaction;
    public EventLifecycle(EventRepository repo,EventChanges changes,PlatformTransactionManager manager){this.repo=repo;this.changes=changes;this.transaction=new TransactionTemplate(manager);}
    @Scheduled(fixedDelayString="${app.lifecycle-delay-ms:10000}") public void advance() {
        transaction.executeWithoutResult(tx->{
            var ids=repo.db().queryForList("select id from event_commerce.events where cancellation_pending=false and ((status='PUBLISHED' and start_at<=clock_timestamp()) or (status='ONGOING' and end_at<=clock_timestamp())) order by id for update skip locked",UUID.class);
            for(UUID id:ids) {
                repo.db().update("update event_commerce.events set status=case when end_at<=clock_timestamp() then 'COMPLETED' else 'ONGOING' end where id=?",id);
                changes.record(id,null,repo.get(id).status(),"Thời gian máy chủ");
            }
        });
    }
}
