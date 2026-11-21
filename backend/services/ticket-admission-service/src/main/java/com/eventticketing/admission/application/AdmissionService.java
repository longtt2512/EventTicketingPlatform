package com.eventticketing.admission.application;
import static com.eventticketing.admission.application.TicketModels.*;
import com.eventticketing.admission.api.ApiException;
import com.eventticketing.admission.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AdmissionService {
 private final JdbcTemplate db;private final SourceClient source;private final IdentityClient identity;private final EventGates gates;private final TicketFacts facts;private final ObjectMapper json;
 public AdmissionService(JdbcTemplate db,SourceClient source,IdentityClient identity,EventGates gates,TicketFacts facts,ObjectMapper json){this.db=db;this.source=source;this.identity=identity;this.gates=gates;this.facts=facts;this.json=json;}
 private void authorize(UUID event,UUID actor){var state=identity.securityState(actor);if(!"ACTIVE".equals(state.status())||!state.emailVerified())throw new ApiException(HttpStatus.FORBIDDEN,"VERIFIED_STAFF_REQUIRED","Cần tài khoản đang hoạt động và email đã xác thực.");source.authorize(event,actor);}
 private Event context(UUID event,UUID actor){authorize(event,actor);var e=source.event(event);gates.snapshot(e);return e;}
 @Transactional public List<EventAccess> events(UUID actor){return source.assigned(actor).stream().sorted().map(id->{var e=context(id,actor);return access(e);}).toList();}
 @Transactional public EventAccess event(UUID id,UUID actor){return access(context(id,actor));}
 private EventAccess access(Event e){Instant now=Instant.now();return new EventAccess(e,e.startAt()==null?null:e.startAt().minusSeconds(7200),e.endAt(),now,window(e,now)&&!gates.frozen(e.id()));}
 private boolean window(Event e,Instant now){return e.startAt()!=null&&e.endAt()!=null&&!now.isBefore(e.startAt().minusSeconds(7200))&&now.isBefore(e.endAt())&&Set.of("PUBLISHED","ONGOING").contains(e.status());}
 @Transactional public LookupResult lookup(UUID event,UUID actor,UUID ticket){var e=context(event,actor);var rows=db.queryForList("select * from ticket_admission.tickets where id=? and event_id=?",ticket,event);if(rows.isEmpty())throw TicketQueries.missing();var r=rows.getFirst();String reason=outcome(e,r);return new LookupResult(ticket,(String)r.get("ticket_type_name"),(String)r.get("buyer_name"),(String)r.get("status"),reason,reason.equals("VALID"),time(r));}
 @Transactional public Decision scan(UUID event,UUID actor,String key,String credential){return consume(event,actor,key,"QR",credential);}
 @Transactional public Decision manual(UUID event,UUID actor,String key,UUID ticket){return consume(event,actor,key,"MANUAL",ticket.toString());}
 private Decision consume(UUID event,UUID actor,String key,String method,String input){key(key);authorize(event,actor);String fingerprint=Credentials.hash(event+"|"+method+"|"+input);db.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))",actor+"|"+key);
  var prior=db.queryForList("select fingerprint,result::text from ticket_admission.admission_requests where actor_id=? and request_key=?",actor,key);if(!prior.isEmpty()){if(!fingerprint.equals(prior.getFirst().get("fingerprint")))throw new ApiException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","Mã yêu cầu đã dùng cho một thao tác khác.");try{var d=json.readValue((String)prior.getFirst().get("result"),Decision.class);return new Decision(d.outcome(),d.eventId(),d.ticketId(),d.checkedInAt(),true);}catch(Exception ex){throw new IllegalStateException(ex);}}
  var e=source.event(event);gates.snapshot(e);
  var rows=method.equals("QR")?db.queryForList("select * from ticket_admission.tickets where credential_hash=?",Credentials.hash(input)):db.queryForList("select * from ticket_admission.tickets where id=?",UUID.fromString(input));
  String reason;UUID ticket=null;Instant when=null;
  if(rows.isEmpty())reason="INVALID_TICKET";
  else if(!event.equals(rows.getFirst().get("event_id")))reason=method.equals("QR")?"WRONG_EVENT":"INVALID_TICKET";
  else{var r=rows.getFirst();ticket=(UUID)r.get("id");reason=outcome(e,r);when=time(r);if(reason.equals("VALID")){
   var times=db.queryForList("update ticket_admission.tickets set status='CHECKED_IN',checked_in_at=clock_timestamp(),version=version+1 where id=? and status='ISSUED' returning checked_in_at",java.sql.Timestamp.class,ticket);
   if(times.size()!=1)throw new IllegalStateException("CHK-R04: conditional admission failed under gate");when=times.getFirst().toInstant();db.update("insert into ticket_admission.admissions(id,ticket_id,event_id,actor_id,method,checked_in_at) values (?,?,?,?,?,?)",UUID.randomUUID(),ticket,event,actor,method,java.sql.Timestamp.from(when));facts.ticket(ticket,"ticket.admission.checked-in.v1");reason="SUCCESS";
  }}
  var decision=new Decision(reason,event,ticket,when,false);try{db.update("insert into ticket_admission.admission_requests(actor_id,request_key,event_id,fingerprint,result) values (?,?,?,?,?::jsonb)",actor,key,event,fingerprint,json.writeValueAsString(decision));}catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}return decision;
 }
 private String outcome(Event e,Map<String,Object> r){if(gates.frozen(e.id()))return "EVENT_CANCELLED";if("VOIDED".equals(r.get("status")))return "VOIDED";if("CHECKED_IN".equals(r.get("status")))return "ALREADY_USED";if(!window(e,Instant.now()))return "OUTSIDE_CHECK_IN_WINDOW";return "VALID";}
 @Transactional public Page<Admission> history(UUID event,UUID actor,UUID cursor,int limit){TicketQueries.limit(limit);context(event,actor);String after="";var args=new ArrayList<Object>();args.add(event);if(cursor!=null){var row=db.queryForList("select checked_in_at,id from ticket_admission.admissions where id=? and event_id=?",cursor,event);if(row.isEmpty())throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_PAGE","Trang lịch sử không hợp lệ.");after=" and (a.checked_in_at,a.id)<(?,?)";args.add(row.getFirst().get("checked_in_at"));args.add(cursor);}args.add(limit+1);var rows=db.query("select a.*,t.ticket_type_name from ticket_admission.admissions a join ticket_admission.tickets t on t.id=a.ticket_id where a.event_id=?"+after+" order by a.checked_in_at desc,a.id desc limit ?",(r,n)->new Admission(r.getObject("id",UUID.class),r.getObject("ticket_id",UUID.class),r.getObject("actor_id",UUID.class),r.getString("method"),r.getTimestamp("checked_in_at").toInstant(),r.getString("ticket_type_name")),args.toArray());boolean more=rows.size()>limit;if(more)rows=rows.subList(0,limit);return new Page<>(rows,more,more?rows.getLast().id().toString():null);}
 @Transactional public Stats stats(UUID event,UUID actor){context(event,actor);var types=db.query("select ticket_type_id,ticket_type_name,count(*) filter(where status='ISSUED') as issued,count(*) filter(where status='CHECKED_IN') as checked,count(*) filter(where status='VOIDED') as voided from ticket_admission.tickets where event_id=? group by ticket_type_id,ticket_type_name order by ticket_type_name",(r,n)->new TypeCount(r.getString("ticket_type_name"),r.getInt("issued"),r.getInt("checked"),r.getInt("voided")),event);int issued=types.stream().mapToInt(TypeCount::issued).sum(),checked=types.stream().mapToInt(TypeCount::checkedIn).sum(),voided=types.stream().mapToInt(TypeCount::voided).sum();return new Stats(issued,checked,voided,gates.frozen(event)?0:issued+checked,Instant.now(),types);}
 private static Instant time(Map<String,Object> r){return r.get("checked_in_at")==null?null:((java.sql.Timestamp)r.get("checked_in_at")).toInstant();}
 private static void key(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{8,128}"))throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_REQUEST_KEY","Mã yêu cầu không hợp lệ.");}
}
