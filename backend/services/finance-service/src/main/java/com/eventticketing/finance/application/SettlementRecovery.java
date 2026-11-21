package com.eventticketing.finance.application;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SettlementRecovery {
    private final JdbcTemplate db;private final SettlementCompletion completion;
    public SettlementRecovery(JdbcTemplate db,SettlementCompletion completion){this.db=db;this.completion=completion;}
    @Scheduled(fixedDelayString="${app.settlement-recovery-delay-ms:10000}")
    public void recover(){
        for(UUID id:db.queryForList("select id from finance.settlements where status='PROCESSING' and decision is not null order by requested_at limit 100",UUID.class)){
            try{completion.complete(id);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).debug("Settlement remains pending verification: {}",id);}
        }
    }
}
