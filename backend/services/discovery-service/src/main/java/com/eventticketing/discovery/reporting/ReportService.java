package com.eventticketing.discovery.reporting;

import com.eventticketing.discovery.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final JdbcTemplate db;private final ObjectMapper json;private final ReportAccess access;private final ReportCalculator calculator;private final PlatformTotals totals;
    public ReportService(JdbcTemplate db,ObjectMapper json,ReportAccess access,ReportCalculator calculator,PlatformTotals totals){this.totals=totals;this.db=db;this.json=json;this.access=access;this.calculator=calculator;}
    public record Filter(Instant from,Instant to,String timezone,String dateBasis,UUID eventId){}
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public ObjectNode snapshot(UUID org,UUID actor,Filter filter,UUID snapshotId){
        access.require(org,actor);ZoneId zone=validate(filter);JsonNode filters=json.valueToTree(filter);
        if(snapshotId!=null){
            var rows=db.queryForList("select actor_id,organization_id,filters::text,payload::text from discovery.report_snapshots where id=?",snapshotId);
            if(rows.isEmpty())throw missing();var row=rows.getFirst();
            if(!actor.equals(row.get("actor_id"))||!Objects.equals(org,row.get("organization_id"))||!filters.equals(read((String)row.get("filters"))))throw missing();return (ObjectNode)read((String)row.get("payload"));
        }
        String where=" where (?::uuid is null or e.organization_id=? or e.event_id in (select id from discovery.report_entities where kind='EVENT' and organization_id=?)) and (?::uuid is null or e.event_id=?)";
        var source=db.queryForList("select kind,payload::text,projected_at from discovery.report_entities e"+where+" order by e.kind,e.id",org,org,org,filter.eventId(),filter.eventId());
        var groups=new HashMap<String,List<JsonNode>>();Instant latest=null;
        for(var row:source){groups.computeIfAbsent((String)row.get("kind"),k->new ArrayList<>()).add(read((String)row.get("payload")));Instant at=((Timestamp)row.get("projected_at")).toInstant();if(latest==null||latest.isBefore(at))latest=at;}
        ObjectNode result;
        try{result=calculator.calculate(groups.getOrDefault("ORDER",List.of()),groups.getOrDefault("EVENT",List.of()),groups.getOrDefault("PAYMENT",List.of()),groups.getOrDefault("SETTLEMENT",List.of()),filter.from(),filter.to(),zone);}
        catch(ArithmeticException ex){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"REPORT_AMOUNT_OVERFLOW","Tổng tiền vượt giới hạn báo cáo VND. Hãy thu hẹp phạm vi.");}
        var attendance=db.queryForMap("select count(*) filter(where a.status<>'VOIDED' and (e.payload->>'status')<>'CANCELLED') as eligible,count(*) filter(where a.status='CHECKED_IN' and (e.payload->>'status')<>'CANCELLED') as admitted,count(*) filter(where a.status='CHECKED_IN' and a.checked_in_at>=? and a.checked_in_at<?) as history,count(*) filter(where a.status='VOIDED') as voided from discovery.event_attendees a join discovery.report_entities e on e.id=a.event_id and e.kind='EVENT' where (?::uuid is null or e.organization_id=?) and (?::uuid is null or a.event_id=?)",
            Timestamp.from(filter.from()),Timestamp.from(filter.to()),org,org,filter.eventId(),filter.eventId());
        var metrics=(ObjectNode)result.path("metrics");metrics.put("checkinsInPeriod",((Number)attendance.get("history")).longValue()).put("voidedTickets",((Number)attendance.get("voided")).longValue());
        metrics.set("checkinPercent",json.valueToTree(ReportCalculator.percent(((Number)attendance.get("admitted")).longValue(),((Number)attendance.get("eligible")).longValue())));
        result.set("sourceStatus",json.valueToTree(db.queryForList("select source,synchronized_at,completed_at,last_error from discovery.reporting_sources").stream().map(row->{var safe=new LinkedHashMap<String,Object>();row.forEach((k,v)->safe.put(k,v instanceof Timestamp t?t.toInstant().toString():v));return safe;}).toList()));
        if(org==null)result.set("platformTotals",json.valueToTree(totals.read()));
        UUID id=UUID.randomUUID();result.put("snapshotId",id.toString()).put("asOf",Instant.now().toString()).put("currency","VND");result.set("filters",filters);
        if(latest==null)result.putNull("dataUpdatedAt");else result.put("dataUpdatedAt",latest.toString());
        result.set("dateBasis",json.valueToTree(Map.of("sales","confirmedAt [from,to)","cash","paidAt [from,to)","returns","returnedAt [from,to)","settlements","completedAt [from,to)","checkins","checkedInAt [from,to)","inventoryAndRates","current projected snapshot", "salesNet","confirmedAt cohort, adjustments observed asOf", "paymentSuccess","definitive outcomeAt [from,to)")));
        db.update("insert into discovery.report_snapshots(id,actor_id,organization_id,filters,payload) values (?,?,?,?::jsonb,?::jsonb)",id,actor,org,filters.toString(),result.toString());return result;
    }
    public ObjectNode summary(ObjectNode snapshot){var result=snapshot.deepCopy();result.remove("rows");return result;}
    public Map<String,Object> sales(ObjectNode snapshot,int offset,int limit){
        if(offset<0||limit<1||limit>100)throw invalid("Trang báo cáo không hợp lệ.");var rows=snapshot.path("rows");var data=new ArrayList<JsonNode>();for(int i=offset;i<Math.min((long)offset+limit,rows.size());i++)data.add(rows.get(i));
        return Map.of("snapshotId",snapshot.path("snapshotId").asText(),"data",data,"total",rows.size(),"hasMore",(long)offset+limit<rows.size());
    }
    @Transactional
    public String csv(ObjectNode snapshot,UUID actor){
        db.update("insert into discovery.report_exports(id,snapshot_id,actor_id) values (?,?,?)",UUID.randomUUID(),UUID.fromString(snapshot.path("snapshotId").asText()),actor);
        StringBuilder out=new StringBuilder("\uFEFFsnapshot_id,as_of,timezone,date_basis,order_id,event_name,confirmed_at,tickets,gross_vnd,platform_fee_vnd,gateway_fee_vnd,adjustments_vnd,net_vnd,finance_status\r\n");
        for(var row:snapshot.path("rows")){
            var values=new ArrayList<String>(List.of(snapshot.path("snapshotId").asText(),snapshot.path("asOf").asText(),snapshot.path("filters").path("timezone").asText(),"confirmedAt"));
            for(String key:List.of("orderId","eventName","confirmedAt","tickets","grossVnd","platformFeeVnd","gatewayFeeVnd","adjustmentsVnd","netVnd","financeStatus"))values.add(row.path(key).isNull()?"PENDING":row.path(key).asText());
            out.append(values.stream().map(ReportService::csvCell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        }
        return out.toString();
    }
    public static String csvCell(String value){String trimmed=value.stripLeading();if(!trimmed.isEmpty()&&("=+-@".indexOf(trimmed.charAt(0))>=0||value.charAt(0)=='\t'||value.charAt(0)=='\r'||value.charAt(0)=='\n'))value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
    private ZoneId validate(Filter f){if(f.from()==null||f.to()==null||!f.from().isBefore(f.to())||!"CONFIRMED_AT".equals(f.dateBasis()))throw invalid("Chọn khoảng thời gian hợp lệ và dateBasis CONFIRMED_AT.");try{return ZoneId.of(f.timezone());}catch(Exception ex){throw invalid("Múi giờ báo cáo không hợp lệ.");}}
    private JsonNode read(String value){try{return json.readTree(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private ApiException invalid(String detail){return new ApiException(HttpStatus.BAD_REQUEST,"REPORT_FILTER_INVALID",detail);}
    private ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"REPORT_SNAPSHOT_NOT_FOUND","Không tìm thấy snapshot đúng người dùng, phạm vi và bộ lọc.");}
}
