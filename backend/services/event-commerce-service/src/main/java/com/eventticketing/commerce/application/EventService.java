package com.eventticketing.commerce.application;

import static com.eventticketing.commerce.application.EventModels.*;
import static com.eventticketing.commerce.application.EventRepository.*;
import com.eventticketing.commerce.infrastructure.OrganizationScopeClient;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private final Categories categories;private final EventRepository repo; private final EventChanges changes;
    private final OrganizationScopeClient scope; private final InventoryService inventory; private final EventCancellation cancellation; private final org.springframework.transaction.support.TransactionTemplate transaction;
    public EventService(EventRepository repo,EventChanges changes,OrganizationScopeClient scope,InventoryService inventory,EventCancellation cancellation,Categories categories,org.springframework.transaction.PlatformTransactionManager manager) {
        this.categories=categories;this.repo=repo;this.changes=changes;this.scope=scope;this.inventory=inventory;this.cancellation=cancellation;this.transaction=new org.springframework.transaction.support.TransactionTemplate(manager);
    }
    public EventView read(UUID org,UUID id,UUID actor,boolean admin) {
        if(!admin)scope.require(org,actor,false); var e=repo.get(id);if(!org.equals(e.organizationId()))throw missing();return e;
    }
    public EventView managed(UUID org,UUID id,UUID actor) {
        scope.require(org,actor,true);var e=repo.lock(id);if(!org.equals(e.organizationId()))throw missing();if(e.cancellationPending())throw invalid("Yêu cầu hủy đang được xử lý.");return e;
    }
    @Transactional public EventView update(UUID org,UUID id,UUID actor,long revision,EventInput input) {
        var e=managed(org,id,actor);version(e.version(),revision);editable(e);validateInput(input,e.category());
        int quota=repo.db().queryForObject("select coalesce(sum(quota),0) from event_commerce.ticket_types where event_id=?",Integer.class,id);
        if(input.capacity()!=null&&input.capacity()<quota)throw invalid("Sức chứa phải lớn hơn hoặc bằng tổng hạn mức vé ("+quota+").");
        if(input.startAt()!=null && e.ticketTypes().stream().anyMatch(t->t.saleEnd().isAfter(input.startAt())))throw invalid("Thời gian đóng bán vé không được sau giờ bắt đầu sự kiện.");
        checkMedia(id,input.bannerMediaId(),"BANNER");
        var gallery=input.galleryMediaIds()==null?List.<UUID>of():input.galleryMediaIds();
        for(UUID media:gallery)checkMedia(id,media,"GALLERY");
        if(gallery.size()!=new HashSet<>(gallery).size())throw invalid("Hình ảnh thư viện bị trùng.");
        repo.db().update("update event_commerce.events set name=?,category=?,description=?,venue_name=?,venue_address=?,city=?,timezone=?,start_at=?,end_at=?,capacity=?,terms=?,banner_media_id=?,gallery_media_ids=?::uuid[],status='DRAFT',review_reason='' where id=?",
            input.name().trim(),emptyToNull(input.category()),text(input.description()),text(input.venueName()),text(input.venueAddress()),text(input.city()),input.timezone()==null?"Asia/Ho_Chi_Minh":input.timezone(),stamp(input.startAt()),stamp(input.endAt()),input.capacity(),text(input.terms()),input.bannerMediaId(),"{"+String.join(",",gallery.stream().map(UUID::toString).toList())+"}",id);
        changes.record(id,actor,"UPDATED","");return repo.get(id);
    }
    public EventView command(UUID org,UUID id,UUID actor,long revision,String action,String reason,boolean admin) {
        if(action.equals("cancel"))return cancellation.cancel(org,id,actor,revision,reason,admin);
        return transaction.execute(tx -> commandLocked(org,id,actor,revision,action,reason,admin));
    }
    private EventView commandLocked(UUID org,UUID id,UUID actor,long revision,String action,String reason,boolean admin) {
        var e=admin?repo.lock(id):managed(org,id,actor);if(!e.organizationId().equals(org))throw missing();if(e.cancellationPending())throw invalid("Yêu cầu hủy đang được xử lý.");version(e.version(),revision);
        String next;
        switch(action) {
            case "submit" -> {state(e,"DRAFT","REJECTED");complete(e);snapshotOrganization(e,actor);next="PENDING_REVIEW";}
            case "approve" -> {state(e,"PENDING_REVIEW");complete(e);next="APPROVED";}
            case "reject" -> {state(e,"PENDING_REVIEW");reasonRequired(reason);next="REJECTED";}
            case "publish" -> {state(e,"APPROVED");complete(e);snapshotOrganization(e,actor);next="PUBLISHED";repo.db().update("update event_commerce.events set published_at=clock_timestamp() where id=?",id);}
            default -> throw invalid("Thao tác không hợp lệ.");
        }
        repo.db().update("update event_commerce.events set status=?,review_reason=? where id=?",next,text(reason),id);
        changes.record(id,actor,next,reason);return repo.get(id);
    }
    @Transactional public EventView feature(UUID id,UUID actor,long revision,boolean featured) {
        var e=repo.lock(id);version(e.version(),revision);state(e,"PUBLISHED");
        repo.db().update("update event_commerce.events set featured=? where id=?",featured,id);changes.record(id,actor,featured?"FEATURED":"UNFEATURED","");return repo.get(id);
    }
    @Transactional public EventView ticket(UUID org,UUID id,UUID actor,long revision,UUID ticketId,TicketInput input) {
        var e=managed(org,id,actor);
        if(!e.status().equals("PUBLISHED"))editable(e);
        TicketView previous=ticketId==null?null:e.ticketTypes().stream().filter(t->t.id().equals(ticketId)).findFirst().orElseThrow(EventRepository::missing);
        version(previous==null?e.version():previous.version(),revision);
        if(previous==null&&e.status().equals("PUBLISHED"))throw invalid("Không thể thêm hạng vé sau khi công khai.");
        if(e.status().equals("PUBLISHED")&&(!previous.name().equals(input.name().trim())||!previous.description().equals(text(input.description()))||!previous.saleStart().equals(input.saleStart())||!previous.saleEnd().equals(input.saleEnd())||previous.maxPerOrder()!=input.maxPerOrder()))
            throw invalid("Sau khi công khai, chỉ có thể đổi giá và hạn mức cho vé mới hoặc tạm dừng bán.");
        if(!input.saleStart().isBefore(input.saleEnd())||e.startAt()==null||input.saleEnd().isAfter(e.startAt()))throw invalid("Chọn thời gian mở bán trước đóng bán; đóng bán không sau giờ bắt đầu sự kiện.");
        if(input.maxPerOrder()>input.quota())throw invalid("Giới hạn mỗi đơn không được lớn hơn hạn mức hạng vé.");
        if(previous!=null&&input.quota()<previous.reserved()+previous.sold())throw invalid("Không thể giảm hạn mức dưới số vé đã bán và đang giữ chỗ.");
        int total=e.ticketTypes().stream().filter(t->!t.id().equals(ticketId)).mapToInt(TicketView::quota).sum();
        if(e.capacity()==null||(long)total+input.quota()>e.capacity())throw invalid("Tổng hạn mức vé vượt sức chứa. Hãy điều chỉnh hạn mức hoặc sức chứa sự kiện.");
        if(ticketId==null)repo.db().update("insert into event_commerce.ticket_types(id,event_id,name,description,price,quota,sale_start,sale_end,max_per_order) values (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),id,input.name().trim(),text(input.description()),input.price(),input.quota(),stamp(input.saleStart()),stamp(input.saleEnd()),input.maxPerOrder());
        else repo.db().update("update event_commerce.ticket_types set name=?,description=?,price=?,quota=?,sale_start=?,sale_end=?,max_per_order=?,version=version+1 where id=?",input.name().trim(),text(input.description()),input.price(),input.quota(),stamp(input.saleStart()),stamp(input.saleEnd()),input.maxPerOrder(),ticketId);
        if(e.status().equals("APPROVED"))repo.db().update("update event_commerce.events set status='DRAFT' where id=?",id);
        changes.record(id,actor,"TICKET_UPDATED",input.name());return repo.get(id);
    }
    @Transactional public EventView pause(UUID org,UUID id,UUID actor,UUID ticketId,long revision,boolean pause) {
        var e=managed(org,id,actor);state(e,"DRAFT","REJECTED","APPROVED","PUBLISHED");
        var ticket=e.ticketTypes().stream().filter(t->t.id().equals(ticketId)).findFirst().orElseThrow(EventRepository::missing);version(ticket.version(),revision);
        if(!pause&&!Instant.now().isBefore(ticket.saleEnd()))throw invalid("Hạng vé đã hết thời gian bán.");
        repo.db().update("update event_commerce.ticket_types set paused=?,version=version+1 where id=?",pause,ticketId);
        if(e.status().equals("APPROVED"))repo.db().update("update event_commerce.events set status='DRAFT' where id=?",id);
        changes.record(id,actor,pause?"TICKET_PAUSED":"TICKET_RESUMED",ticket.name());return repo.get(id);
    }
    public List<Map<String,Object>> history(UUID id){return repo.db().queryForList("select action,reason,version,occurred_at as \"occurredAt\" from event_commerce.event_audit where event_id=? order by version desc",id);}
    public Map<String,Object> reviewQueue(String status,String cursor,int limit) {
        if(status!=null&&!Set.of("PENDING_REVIEW","APPROVED","REJECTED","PUBLISHED","ONGOING","COMPLETED","CANCELLED","DRAFT").contains(status))throw invalid("Trạng thái không hợp lệ.");
        if(limit<1||limit>100)throw invalid("Giới hạn trang không hợp lệ.");
        String after="";List<Object> args=new ArrayList<>();args.add(status);args.add(status);
        if(cursor!=null&&!cursor.isBlank())try {
            String[] parts=new String(Base64.getUrlDecoder().decode(cursor),java.nio.charset.StandardCharsets.UTF_8).split("\\|",-1);
            if(parts.length!=2)throw new IllegalArgumentException();args.add(stamp(Instant.parse(parts[0])));args.add(UUID.fromString(parts[1]));after=" and (updated_at,id)<(?,?)";
        }catch(RuntimeException ex){throw invalid("Trang không hợp lệ. Hãy tải lại danh sách.");}
        args.add(limit+1);
        var ids=repo.db().queryForList("select id from event_commerce.events where (?::text is null or status=?)"+after+" order by updated_at desc,id desc limit ?",UUID.class,args.toArray());
        boolean more=ids.size()>limit;if(more)ids=ids.subList(0,limit);var data=ids.stream().map(repo::get).toList();
        String next=null;if(more){var last=data.getLast();next=Base64.getUrlEncoder().withoutPadding().encodeToString((last.updatedAt()+"|"+last.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        var result=new LinkedHashMap<String,Object>();result.put("data",data);result.put("hasMore",more);result.put("nextCursor",next);return result;
    }
    private void snapshotOrganization(EventView e,UUID actor) {
        var profile=scope.details(e.organizationId(),actor,true);
        if(profile!=null)repo.db().update("update event_commerce.events set organization_name=?,organization_description=? where id=?",profile.name(),profile.description(),e.id());
    }
    private void complete(EventView e) {
        List<String> missing=new ArrayList<>();
        if(e.category()==null)missing.add("danh mục");if(e.description().isBlank())missing.add("mô tả");if(e.bannerMediaId()==null)missing.add("ảnh bìa");
        if(e.venueName().isBlank()||e.venueAddress().isBlank()||e.city().isBlank())missing.add("địa điểm và thành phố");
        if(e.startAt()==null||e.endAt()==null)missing.add("thời gian");if(e.capacity()==null)missing.add("sức chứa");if(e.terms().isBlank())missing.add("điều khoản");if(e.ticketTypes().isEmpty())missing.add("ít nhất một hạng vé");
        if(!missing.isEmpty())throw invalid("Hãy bổ sung: "+String.join(", ",missing)+".");
        if(!e.startAt().isAfter(Instant.now()))throw invalid("Sự kiện phải bắt đầu trong tương lai.");
        checkMedia(e.id(),e.bannerMediaId(),"BANNER");
    }
    private void checkMedia(UUID event,UUID id,String kind) {
        if(id!=null&&!Boolean.TRUE.equals(repo.db().queryForObject("select exists(select 1 from event_commerce.media_uploads where id=? and event_id=? and kind=? and status='AVAILABLE')",Boolean.class,id,event,kind)))throw invalid("Ảnh chưa tải xong hoặc không thuộc sự kiện này. Hãy tải lại ảnh.");
    }
    private void validateInput(EventInput i,String currentCategory) {
        categories.requireNewSelection(emptyToNull(i.category()),currentCategory);
        if(i.startAt()!=null&&i.endAt()!=null&&!i.startAt().isBefore(i.endAt()))throw invalid("Giờ kết thúc phải sau giờ bắt đầu.");
        try {ZoneId.of(i.timezone()==null?"Asia/Ho_Chi_Minh":i.timezone());}catch(DateTimeException ex){throw invalid("Múi giờ không hợp lệ.");}
    }
    public static void editable(EventView e){state(e,"DRAFT","REJECTED","APPROVED");}
    public static void state(EventView e,String... allowed){if(!Arrays.asList(allowed).contains(e.status()))throw invalid("Không thể thực hiện ở trạng thái "+e.status()+". Hãy tải lại trạng thái sự kiện.");}
    private static void reasonRequired(String reason){if(reason==null||reason.isBlank())throw invalid("Nhập lý do để tiếp tục.");}
    private static String emptyToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private static String text(String value){return value==null?"":value.trim();}
    public static Timestamp stamp(Instant value){return value==null?null:Timestamp.from(value);}
}
