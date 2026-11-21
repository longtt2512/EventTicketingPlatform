package com.eventticketing.admission.application;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class IssuanceWorker {
 private final JdbcTemplate db;private final IssuanceService issuance;
 public IssuanceWorker(JdbcTemplate db,IssuanceService issuance){this.db=db;this.issuance=issuance;}
 @Scheduled(fixedDelayString="${app.issuance-delay-ms:1000}",initialDelayString="${app.issuance-delay-ms:1000}") public void run(){for(UUID id:db.queryForList("select order_id from ticket_admission.issuance_obligations where state='PENDING' and available_at<=clock_timestamp() order by created_at limit 10",UUID.class))try{issuance.fulfill(id);}catch(RuntimeException ex){db.update("update ticket_admission.issuance_obligations set attempts=attempts+1,available_at=clock_timestamp()+interval '10 seconds' where order_id=? and state='PENDING'",id);org.slf4j.LoggerFactory.getLogger(getClass()).warn("Issuance deferred for order {}",id);}}
}
