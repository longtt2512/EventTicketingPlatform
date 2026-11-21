package com.eventticketing.commerce.application;
import static com.eventticketing.commerce.application.EventRepository.*;
import com.eventticketing.commerce.api.ApiException;
import com.eventticketing.commerce.infrastructure.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@Service
public class EventCancellation {
 private final EventRepository repo;private final EventChanges changes;private final InventoryService inventory;private final OrganizationScopeClient scope;private final AdmissionFenceClient admission;private final TransactionTemplate tx;
 public EventCancellation(EventRepository repo,EventChanges changes,InventoryService inventory,OrganizationScopeClient scope,AdmissionFenceClient admission,PlatformTransactionManager manager){this.repo=repo;this.changes=changes;this.inventory=inventory;this.scope=scope;this.admission=admission;tx=new TransactionTemplate(manager);}
 public EventModels.EventView cancel(UUID org,UUID event,UUID actor,long revision,String reason,boolean admin){
  tx.executeWithoutResult(t->{if(!admin)scope.require(org,actor,true);var e=repo.lock(event);if(!org.equals(e.organizationId()))throw missing();if(!repo.db().queryForList("select event_id from event_commerce.cancellation_requests where event_id=?",event).isEmpty())return;version(e.version(),revision);if(Set.of("CANCELLED","COMPLETED").contains(e.status()))throw invalid("Sự kiện đã kết thúc hoặc đã hủy.");if(reason==null||reason.isBlank())throw invalid("Nhập lý do để tiếp tục.");repo.db().update("insert into event_commerce.cancellation_requests(event_id,actor_id,reason) values (?,?,?)",event,actor,reason.trim());repo.db().update("update event_commerce.events set cancellation_pending=true where id=?",event);inventory.releaseAll(event);changes.record(event,actor,"CANCEL_REQUESTED",reason);});
  try{complete(event);}catch(RuntimeException ex){defer(event);throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"CANCELLATION_PENDING","Yêu cầu hủy đã được lưu và dừng bán. Hệ thống đang khóa check-in; hãy tải lại trạng thái sau.");}return repo.get(event);
 }
 public void complete(UUID event){if(repo.db().queryForList("select event_id from event_commerce.cancellation_requests where event_id=? and completed_at is null",event).isEmpty())return;admission.freeze(event);tx.executeWithoutResult(t->{repo.lock(event);var rows=repo.db().queryForList("select * from event_commerce.cancellation_requests where event_id=? and completed_at is null for update",event);if(rows.isEmpty())return;var r=rows.getFirst();repo.db().update("update event_commerce.events set status='CANCELLED',cancellation_pending=false,review_reason=? where id=?",r.get("reason"),event);changes.record(event,(UUID)r.get("actor_id"),"CANCELLED",(String)r.get("reason"));repo.db().update("update event_commerce.cancellation_requests set completed_at=clock_timestamp(),attempts=attempts+1 where event_id=?",event);});}
 @Scheduled(fixedDelayString="${app.cancellation-delay-ms:2000}",initialDelayString="${app.cancellation-delay-ms:2000}") public void reconcile(){for(UUID id:repo.db().queryForList("select event_id from event_commerce.cancellation_requests where completed_at is null and available_at<=clock_timestamp() order by created_at limit 10",UUID.class))try{complete(id);}catch(RuntimeException ex){defer(id);}}
 private void defer(UUID id){repo.db().update("update event_commerce.cancellation_requests set attempts=attempts+1,available_at=clock_timestamp()+interval '10 seconds' where event_id=? and completed_at is null",id);}
}
