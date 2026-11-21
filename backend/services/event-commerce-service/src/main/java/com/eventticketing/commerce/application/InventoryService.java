package com.eventticketing.commerce.application;

import static com.eventticketing.commerce.application.EventRepository.*;
import static com.eventticketing.commerce.application.EventService.stamp;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal allocation primitive. Phase 3 invokes this inside the same order transaction. No public reservation API. */
@Service
public class InventoryService {
    private final EventRepository repo;private final EventChanges changes;private final com.eventticketing.commerce.infrastructure.OrganizationScopeClient scope;
    public InventoryService(EventRepository repo,EventChanges changes,com.eventticketing.commerce.infrastructure.OrganizationScopeClient scope){this.repo=repo;this.changes=changes;this.scope=scope;}
    @Transactional public void reserve(UUID eventId,UUID allocationId,Map<UUID,Integer> quantities,Instant expiresAt) {
        var event=repo.lock(eventId);
        var existing=repo.db().queryForList("select event_id from event_commerce.inventory_allocations where id=?",allocationId);
        if(!existing.isEmpty()) {
            var prior=repo.db().query("select ticket_type_id,quantity from event_commerce.allocation_lines where allocation_id=?",(r,n)->Map.entry(r.getObject(1,UUID.class),r.getInt(2)),allocationId);
            var priorMap=new HashMap<UUID,Integer>();prior.forEach(x->priorMap.put(x.getKey(),x.getValue()));
            if(!eventId.equals(existing.getFirst().get("event_id"))||!priorMap.equals(quantities))throw invalid("Mã giữ chỗ đã được dùng với nội dung khác.");
            return;
        }
        scope.active(event.organizationId());if(event.cancellationPending())throw invalid("Sự kiện đang xử lý hủy, không thể giữ chỗ mới.");
        if(quantities.isEmpty()||expiresAt==null||!expiresAt.isAfter(Instant.now()))throw invalid("Giữ chỗ phải có vé và hạn còn hiệu lực.");
        for(var line:quantities.entrySet()) {
            var ticket=event.ticketTypes().stream().filter(t->t.id().equals(line.getKey())).findFirst().orElseThrow(EventRepository::missing);
            if(!ticket.saleState().equals("ON_SALE")||line.getValue()==null||line.getValue()<=0||line.getValue()>ticket.maxPerOrder()||line.getValue()>ticket.available())throw invalid("Hạng vé chưa mở bán hoặc không đủ số lượng. Hãy chọn lại vé.");
        }
        repo.db().update("insert into event_commerce.inventory_allocations(id,event_id,expires_at,state) values (?,?,?,'ACTIVE')",allocationId,eventId,stamp(expiresAt));
        for(var line:quantities.entrySet()) {
            repo.db().update("update event_commerce.ticket_types set reserved=reserved+? where id=?",line.getValue(),line.getKey());
            repo.db().update("insert into event_commerce.allocation_lines(allocation_id,ticket_type_id,quantity,price_snapshot) select ?,id,?,price from event_commerce.ticket_types where id=?",allocationId,line.getValue(),line.getKey());
        }
        ledger(allocationId,"RESERVED");changes.record(eventId,null,"INVENTORY_RESERVED","");
    }
    @Transactional public void close(UUID eventId,UUID allocationId,String target) {
        var event=repo.lock(eventId);
        var rows=repo.db().queryForList("select state,expires_at from event_commerce.inventory_allocations where id=? and event_id=? for update",allocationId,eventId);
        if(rows.isEmpty())throw missing();var row=rows.getFirst();
        if(target.equals(row.get("state")))return;
        if(!"ACTIVE".equals(row.get("state")))throw invalid("Giữ chỗ đã được đóng.");
        Instant expires=((java.sql.Timestamp)row.get("expires_at")).toInstant();
        if(!Set.of("CONSUMED","RELEASED","EXPIRED").contains(target))throw invalid("Chuyển trạng thái tồn kho không hợp lệ.");
        if(target.equals("CONSUMED")&&(event.cancellationPending()||!event.status().equals("PUBLISHED")||!Instant.now().isBefore(expires)))throw invalid("Giữ chỗ đã hết hạn hoặc sự kiện không còn bán vé.");
        if(target.equals("EXPIRED")&&Instant.now().isBefore(expires))throw invalid("Giữ chỗ chưa hết hạn.");
        repo.db().update("update event_commerce.ticket_types t set reserved=t.reserved-l.quantity,sold=t.sold+case when ? then l.quantity else 0 end from event_commerce.allocation_lines l where l.ticket_type_id=t.id and l.allocation_id=?",target.equals("CONSUMED"),allocationId);
        repo.db().update("update event_commerce.inventory_allocations set state=? where id=?",target,allocationId);
        ledger(allocationId,target);changes.record(eventId,null,"INVENTORY_"+target,"");
    }
    public void releaseAll(UUID eventId) {
        repo.db().queryForList("select id from event_commerce.inventory_allocations where event_id=? and state='ACTIVE'",UUID.class,eventId).forEach(id->close(eventId,id,"RELEASED"));
    }
    private void ledger(UUID id,String action){repo.db().update("insert into event_commerce.allocation_ledger(allocation_id,transition) values (?,?)",id,action);}
}
