package com.eventticketing.admission.application;
import static com.eventticketing.admission.application.TicketModels.*;
import com.eventticketing.admission.api.ApiException;
import com.eventticketing.admission.infrastructure.SourceClient;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketQueries {
 private final JdbcTemplate db;private final EventGates gates;private final Credentials credentials;private final SourceClient source;
 public TicketQueries(JdbcTemplate db,EventGates gates,Credentials credentials,SourceClient source){this.db=db;this.gates=gates;this.credentials=credentials;this.source=source;}
 @Transactional(readOnly=true) public Page<Ticket> list(UUID actor,UUID order,UUID cursor,int limit){limit(limit);var rows=db.query("select * from ticket_admission.tickets where customer_id=? and (?::uuid is null or order_id=?) and (?::uuid is null or id>?) order by id limit ?",(r,n)->map(r,false),actor,order,order,cursor,cursor,limit+1);boolean more=rows.size()>limit;if(more)rows=rows.subList(0,limit);return new Page<>(rows,more,more?rows.getLast().id().toString():null);}
 @Transactional public Ticket detail(UUID id,UUID actor){var rows=db.queryForList("select event_id from ticket_admission.tickets where id=? and customer_id=?",UUID.class,id,actor);if(rows.isEmpty())throw missing();gates.lock(rows.getFirst());return db.query("select * from ticket_admission.tickets where id=? and customer_id=?",(r,n)->map(r,true),id,actor).stream().findFirst().orElseThrow(TicketQueries::missing);}
 @Transactional(readOnly=true) public Progress progress(UUID order,UUID actor){var o=source.order(order);if(!o.customerId().equals(actor))throw missing();int expected=o.items().stream().mapToInt(Item::quantity).sum();var tickets=list(actor,order,null,100).data();int issued=db.queryForObject("select count(*) from ticket_admission.tickets where order_id=?",Integer.class,order);var state=db.queryForList("select state from ticket_admission.issuance_obligations where order_id=?",String.class,order);String status=!"CONFIRMED".equals(o.status())?"NOT_ELIGIBLE":"CANCELLED".equals(o.eventStatus())||state.contains("CANCELLED")?"CANCELLED":issued==expected?"READY":"ISSUING";return new Progress(order,status,expected,issued,tickets);}
 private Ticket map(ResultSet r,boolean secret)throws SQLException{UUID id=r.getObject("id",UUID.class),event=r.getObject("event_id",UUID.class);var e=gates.event(event);boolean cancelled=gates.frozen(event);String status=r.getString("status"),qr=null;if(secret&&!cancelled&&status.equals("ISSUED")&&e.endAt()!=null&&Instant.now().isBefore(e.endAt()))qr=TicketQr.dataUri(credentials.decrypt(id,r.getString("credential_ciphertext")));return new Ticket(id,r.getObject("order_id",UUID.class),event,e.name(),e.venueName(),e.venueAddress(),e.timezone(),e.startAt(),e.endAt(),r.getString("ticket_type_name"),r.getString("buyer_name"),status,cancelled,r.getTimestamp("issued_at").toInstant(),r.getTimestamp("checked_in_at")==null?null:r.getTimestamp("checked_in_at").toInstant(),r.getLong("version"),qr);}
 public static void limit(int limit){if(limit<1||limit>100)throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_PAGE","Giới hạn trang phải từ 1 đến 100.");}
 public static ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"TICKET_NOT_FOUND","Không tìm thấy vé hoặc bạn không có quyền xem.");}
}
