package com.eventticketing.commerce.application;

import static com.eventticketing.commerce.application.EventModels.*;
import com.eventticketing.commerce.api.ApiException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepository {
    private final JdbcTemplate db;
    public JdbcTemplate db(){return db;}
    public EventRepository(JdbcTemplate db) { this.db=db; }
    public EventView get(UUID id) {
        return db.query("select * from event_commerce.events where id=?",this::map,id).stream().findFirst().orElseThrow(EventRepository::missing);
    }
    public EventView lock(UUID id) {
        if(db.queryForList("select id from event_commerce.events where id=? for update",id).isEmpty()) throw missing();
        return get(id);
    }
    public List<EventView> list(UUID org) { return db.query("select * from event_commerce.events where organization_id=? order by created_at desc,id",this::map,org); }
    public List<TicketView> tickets(UUID id,String state) {
        return db.query("select * from event_commerce.ticket_types where event_id=? order by created_at,id",(r,n)-> {
            int available=r.getInt("quota")-r.getInt("reserved")-r.getInt("sold");
            Instant start=r.getTimestamp("sale_start").toInstant(),end=r.getTimestamp("sale_end").toInstant(),now=Instant.now();
            String sale=!state.equals("PUBLISHED")?"UNAVAILABLE":r.getBoolean("paused")?"PAUSED":now.isBefore(start)?"NOT_STARTED":!now.isBefore(end)?"CLOSED":available==0?"SOLD_OUT":"ON_SALE";
            return new TicketView(r.getObject("id",UUID.class),r.getString("name"),r.getString("description"),r.getLong("price"),r.getInt("quota"),r.getInt("reserved"),r.getInt("sold"),available,start,end,r.getInt("max_per_order"),r.getBoolean("paused"),r.getLong("version"),sale);
        },id);
    }
    private EventView map(ResultSet r,int n) throws SQLException {
        String status=r.getString("status");
        Instant start=instant(r,"start_at"),end=instant(r,"end_at"),now=Instant.now();
        if(Set.of("PUBLISHED","ONGOING").contains(status)) status=end!=null&&!now.isBefore(end)?"COMPLETED":start!=null&&!now.isBefore(start)?"ONGOING":status;
        UUID id=r.getObject("id",UUID.class);
        var gallery=Arrays.stream((Object[])r.getArray("gallery_media_ids").getArray()).map(v->UUID.fromString(v.toString())).toList();
        return new EventView(id,r.getObject("organization_id",UUID.class),r.getString("name"),status,r.getLong("version"),r.getString("category"),r.getString("description"),r.getString("venue_name"),r.getString("venue_address"),r.getString("city"),r.getString("timezone"),start,end,(Integer)r.getObject("capacity"),r.getString("terms"),r.getObject("banner_media_id",UUID.class),gallery,r.getBoolean("featured"),instant(r,"published_at"),r.getString("review_reason"),r.getString("organization_name"),r.getString("organization_description"),instant(r,"updated_at"),tickets(id,r.getBoolean("cancellation_pending")?"UNAVAILABLE":status),r.getBoolean("cancellation_pending"));
    }
    private static Instant instant(ResultSet r,String name) throws SQLException {var t=r.getTimestamp(name);return t==null?null:t.toInstant();}
    public static ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Không tìm thấy sự kiện hoặc bạn không có quyền truy cập.");}
    public static ApiException invalid(String text){return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"EVENT_VALIDATION",text);}
    public static void version(long actual,long expected){if(actual!=expected)throw new ApiException(HttpStatus.CONFLICT,"STALE_VERSION","Dữ liệu đã thay đổi. Tải lại trước khi tiếp tục.");}
}
