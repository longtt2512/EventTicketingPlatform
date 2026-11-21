package com.eventticketing.discovery;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/discover")
public class DiscoveryController {
    private final NamedParameterJdbcTemplate db;private final ObjectMapper json;
    public DiscoveryController(NamedParameterJdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @GetMapping("/categories") public List<String> categories(){return List.of("Music & Concert","Sports","Conference","Workshop","Exhibition","Festival","Entertainment","Others");}
    @GetMapping("/events") public Map<String,Object> search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String category,@RequestParam(defaultValue="") String city,
        @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,@RequestParam(required=false) Long minPrice,@RequestParam(required=false) Long maxPrice,
        @RequestParam(required=false) Boolean featured,@RequestParam(defaultValue="upcoming") String sort,@RequestParam(required=false) String cursor,@RequestParam(defaultValue="24") int limit) {
        if(!Set.of("upcoming","popular").contains(sort)||limit<1||limit>100||q.length()>200||city.length()>100||(from!=null&&to!=null&&from.isAfter(to))||(minPrice!=null&&minPrice<0)||(maxPrice!=null&&maxPrice<0)||(minPrice!=null&&maxPrice!=null&&minPrice>maxPrice))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Điều kiện tìm kiếm không hợp lệ.");
        Map<String,Object> params=new HashMap<>();params.put("limit",limit+1);
        StringBuilder where=new StringBuilder("status='PUBLISHED' and start_at>clock_timestamp()");
        if(!q.isBlank()){where.append(" and name ilike :q escape '\\'");params.put("q","%"+escape(q.trim())+"%");}
        if(!category.isBlank()){where.append(" and category=:category");params.put("category",category);}
        if(!city.isBlank()){where.append(" and city ilike :city escape '\\'");params.put("city","%"+escape(city.trim())+"%");}
        if(from!=null){where.append(" and start_at>=:from");params.put("from",Timestamp.from(from));}
        if(to!=null){where.append(" and start_at<=:to");params.put("to",Timestamp.from(to));}
        if(featured!=null){where.append(" and featured=:featured");params.put("featured",featured);}
        if(minPrice!=null||maxPrice!=null){where.append(" and exists(select 1 from jsonb_array_elements(payload->'ticketTypes') t where (t->>'price')::bigint between :min and :max)");params.put("min",minPrice==null?0:minPrice);params.put("max",maxPrice==null?Long.MAX_VALUE:maxPrice);}
        boolean popular=sort.equals("popular");
        if(cursor!=null&&!cursor.isBlank()) {
            try {
                String[] parts=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8).split("\\|",-1);
                if(parts.length!=4||!parts[0].equals(sort))throw new IllegalArgumentException();
                params.put("afterSold",Long.parseLong(parts[1]));params.put("afterTime",Timestamp.from(Instant.parse(parts[2])));params.put("afterId",UUID.fromString(parts[3]));
                where.append(popular?" and (sold<:afterSold or (sold=:afterSold and (start_at,id)>(:afterTime,:afterId)))":" and (start_at,id)>(:afterTime,:afterId)");
            }catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trang tìm kiếm không hợp lệ. Hãy tìm lại.");}
        }
        var rows=db.queryForList("select payload::text,projected_at,source_updated_at,sold,start_at,id from discovery.public_events where "+where+" order by "+(popular?"sold desc,":"")+"start_at,id limit :limit",params);
        boolean more=rows.size()>limit;if(more)rows=rows.subList(0,limit);
        List<JsonNode> data=new ArrayList<>();for(var row:rows)data.add(view(row));
        String next=null;if(more){var last=rows.getLast();next=Base64.getUrlEncoder().withoutPadding().encodeToString((sort+"|"+last.get("sold")+"|"+((Timestamp)last.get("start_at")).toInstant()+"|"+last.get("id")).getBytes(StandardCharsets.UTF_8));}
        var result=new LinkedHashMap<String,Object>();result.put("data",data);result.put("hasMore",more);result.put("nextCursor",next);return result;
    }
    @GetMapping("/events/{id}") public JsonNode detail(@PathVariable UUID id) {
        var rows=db.queryForList("select payload::text,projected_at,source_updated_at from discovery.public_events where id=:id",Map.of("id",id));
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sự kiện công khai.");return view(rows.getFirst());
    }
    private JsonNode view(Map<String,Object> row) {
        try {
            ObjectNode data=(ObjectNode)json.readTree((String)row.get("payload"));Instant now=Instant.now();String state=data.path("status").asText();
            if(!state.equals("CANCELLED"))state=!now.isBefore(Instant.parse(data.path("endAt").asText()))?"COMPLETED":!now.isBefore(Instant.parse(data.path("startAt").asText()))?"ONGOING":state;
            data.put("status",state);data.put("projectedAt",((Timestamp)row.get("projected_at")).toInstant().toString());data.put("sourceUpdatedAt",((Timestamp)row.get("source_updated_at")).toInstant().toString());
            Long minimum=null;
            for(JsonNode t:data.path("ticketTypes")) {
                String sale=!state.equals("PUBLISHED")?"UNAVAILABLE":t.path("paused").asBoolean()?"PAUSED":now.isBefore(Instant.parse(t.path("saleStart").asText()))?"NOT_STARTED":!now.isBefore(Instant.parse(t.path("saleEnd").asText()))?"CLOSED":t.path("available").asInt()==0?"SOLD_OUT":"ON_SALE";
                ((ObjectNode)t).put("saleState",sale);long price=t.path("price").asLong();minimum=minimum==null?price:Math.min(minimum,price);
            }
            if(minimum==null)data.putNull("minPrice");else data.put("minPrice",minimum);
            return data;
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}
    }
    private String escape(String text){return text.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
    @ExceptionHandler({ResponseStatusException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String,Object>> error(Exception exception) {
        var status=exception instanceof ResponseStatusException e?e.getStatusCode():HttpStatus.BAD_REQUEST;
        String detail=exception instanceof ResponseStatusException e?Objects.toString(e.getReason(),"Yêu cầu không hợp lệ."):"Điều kiện tìm kiếm không hợp lệ.";
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(Map.of("status",status.value(),"code",status.value()==404?"RESOURCE_NOT_FOUND":"VALIDATION_ERROR","detail",detail));
    }
}
