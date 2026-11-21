package com.eventticketing.payment.application;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service
public class ReconciliationRunner {
 private final FinancialLedger financial;private final JdbcTemplate db;private final ObjectProvider<MockProvider> mock;
 public ReconciliationRunner(JdbcTemplate db,ObjectProvider<MockProvider> mock,FinancialLedger financial){this.financial=financial;this.db=db;this.mock=mock;}
 public Map<String,Object> run(UUID actor){UUID id=UUID.randomUUID();db.update("insert into payment.reconciliation_runs(id,actor_id) values (?,?)",id,actor);int count=query();financial.bootstrapUnmapped();financial.reconcile();db.update("update payment.reconciliation_runs set examined=?,completed_at=clock_timestamp() where id=?",count,id);return Map.of("id",id,"examined",count);}
 @Scheduled(fixedDelayString="${app.reconciliation-delay-ms:30000}")public void periodic(){query();financial.bootstrapUnmapped();financial.reconcile();}
 private int query(){var provider=mock.getIfAvailable();if(provider==null)return 0;var ids=db.queryForList("select id from payment.attempts where status='PENDING' order by updated_at,id limit 100",UUID.class);for(UUID id:ids){provider.query(id);db.update("update payment.attempts set updated_at=clock_timestamp() where id=?",id);}return ids.size();}
}
