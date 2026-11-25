package com.eventticketing.finance.application;

import static com.eventticketing.finance.application.CommissionModels.*;
import com.eventticketing.finance.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public CommissionService(JdbcTemplate db, ObjectMapper json) { this.db=db; this.json=json; }

    @Transactional(readOnly=true)
    public Map<String,Object> configuration() {
        return Map.of("version",db.queryForObject("select version from finance.configuration_gate where id=1",Long.class),
            "current",current(now()), "serverTime",now());
    }
    @Transactional(readOnly=true)
    public Map<String,Object> history(Long before, int limit) {
        if(limit<1||limit>100) throw invalid("Giới hạn trang từ 1 đến 100.");
        var rows=db.query("select * from finance.commission_configs where version<? order by version desc limit ?",
            (r,n)->new Configuration(r.getObject("id",UUID.class),r.getString("mode"),(Integer)r.getObject("rate_bps"),
                (Long)r.getObject("fixed_fee_vnd"),r.getTimestamp("effective_at").toInstant(),r.getLong("version"),r.getString("reason")),
            before==null?Long.MAX_VALUE:before,limit+1);
        var result=new LinkedHashMap<String,Object>();boolean more=rows.size()>limit;
        result.put("data",rows.stream().limit(limit).toList());result.put("hasMore",more);
        result.put("nextCursor",more?rows.get(limit-1).version():null);return result;
    }
    @Transactional
    public Map<String,Object> change(Change change, long expected, UUID actor) {
        long version=lock();
        if(version!=expected) throw conflict("STALE_VERSION","Cấu hình phí đã đổi. Tải lại và xác nhận mức phí mới.");
        validate(change.mode(),change.rateBps(),change.fixedFeeVnd());
        Instant now=now(), effective=change.effectiveAt()==null?now:change.effectiveAt();
        if(effective.isBefore(now)) throw invalid("Không được áp phí ngược về quá khứ.");
        var previous=configuration();UUID id=UUID.randomUUID();
        db.update("insert into finance.commission_configs(id,mode,rate_bps,fixed_fee_vnd,effective_at,created_by,reason,version) values (?,?,?,?,?,?,?,?)",
            id,change.mode(),change.rateBps(),change.fixedFeeVnd(),Timestamp.from(effective),actor,change.reason().strip(),version+1);
        db.update("update finance.configuration_gate set version=version+1 where id=1");
        db.update("insert into finance.audit(id,resource_id,actor_id,action,before_value,after_value,reason) values (?,?,?,'COMMISSION_CHANGED',?::jsonb,?::jsonb,?)",
            UUID.randomUUID(),id,actor,write(previous),write(change),change.reason().strip());
        return configuration();
    }
    @Transactional
    public Snapshot quote(QuoteRequest request) {
        lock(); // serialize configuration activation/quote and duplicate requests
        var sorted=request.lines().stream().sorted(Comparator.comparing(Line::ticketTypeId)).toList();
        if(sorted.stream().map(Line::ticketTypeId).distinct().count()!=sorted.size()) throw invalid("Mỗi hạng vé chỉ có một dòng.");
        String fingerprint=hash(write(sorted));
        var prior=db.queryForList("select request_hash,snapshot::text from finance.commission_quotes where order_id=?",request.orderId());
        if(!prior.isEmpty()) {
            if(!fingerprint.equals(prior.getFirst().get("request_hash"))) throw conflict("IDEMPOTENCY_CONFLICT","Đơn đã có snapshot phí với nội dung khác.");
            return read((String)prior.getFirst().get("snapshot"),Snapshot.class);
        }
        Instant captured=now();var config=current(captured);var lines=new ArrayList<FeeLine>();long total=0,fee=0;
        try {
            for(var line:sorted) {
                if(line.unitPrice()<0||line.quantity()<1)throw invalid("Giá và số lượng không hợp lệ.");
                long per=feePerTicket(line.unitPrice(),config.mode(),config.rateBps(),config.fixedFeeVnd());
                long amount=Math.multiplyExact(per,line.quantity());
                total=Math.addExact(total,Math.multiplyExact(line.unitPrice(),line.quantity()));fee=Math.addExact(fee,amount);
                lines.add(new FeeLine(line.ticketTypeId(),line.unitPrice(),line.quantity(),per,amount));
            }
        } catch(ArithmeticException ex) { throw invalid("Tổng tiền vượt giới hạn VND. Giảm số lượng hoặc giá vé."); }
        var snapshot=new Snapshot(config.id(),config.mode(),config.rateBps(),config.fixedFeeVnd(),captured,total,fee,List.copyOf(lines));
        db.update("insert into finance.commission_quotes(order_id,request_hash,snapshot,captured_at) values (?,?,?::jsonb,?)",
            request.orderId(),fingerprint,write(snapshot),Timestamp.from(captured));return snapshot;
    }
    public static long feePerTicket(long price,String mode,Integer bps,Long fixed) {
        validate(mode,bps,fixed);
        if(price<0)throw invalid("Giá vé không được âm.");
        if(price==0)return 0;
        long calculated=mode.equals("FIXED_AMOUNT")?fixed:BigInteger.valueOf(price).multiply(BigInteger.valueOf(bps))
            .add(BigInteger.valueOf(5000)).divide(BigInteger.valueOf(10000)).longValueExact();
        return Math.min(price,calculated);
    }
    private static void validate(String mode,Integer bps,Long fixed) {
        if(!("PERCENTAGE".equals(mode)&&bps!=null&&bps>=0&&bps<=10000&&fixed==null)
            &&!("FIXED_AMOUNT".equals(mode)&&fixed!=null&&fixed>=0&&bps==null))
            throw invalid("Chọn phần trăm 0–100% hoặc phí cố định không âm. Không kết hợp hai kiểu.");
    }
    private Configuration current(Instant at) {
        return db.query("select * from finance.commission_configs where effective_at<=? order by effective_at desc limit 1",
            (r,n)->new Configuration(r.getObject("id",UUID.class),r.getString("mode"),(Integer)r.getObject("rate_bps"),
                (Long)r.getObject("fixed_fee_vnd"),r.getTimestamp("effective_at").toInstant(),r.getLong("version"),r.getString("reason")),Timestamp.from(at))
            .stream().findFirst().orElseThrow(()->new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"COMMISSION_UNAVAILABLE","Chưa có mức phí hiệu lực. Thử lại sau khi Admin cấu hình."));
    }
    private long lock() { return db.queryForObject("select version from finance.configuration_gate where id=1 for update",Long.class); }
    private Instant now() { return db.queryForObject("select clock_timestamp()",Timestamp.class).toInstant(); }
    private String write(Object value) { try{return json.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);} }
    private <T>T read(String value,Class<T> type) { try{return json.readValue(value,type);}catch(Exception ex){throw new IllegalStateException(ex);} }
    private String hash(String value) { try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);} }
    public static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",message); }
    public static ApiException conflict(String code,String message) { return new ApiException(HttpStatus.CONFLICT,code,message); }
}
