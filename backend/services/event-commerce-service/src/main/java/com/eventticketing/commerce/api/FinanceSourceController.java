package com.eventticketing.commerce.api;

import com.eventticketing.commerce.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/finance")
public class FinanceSourceController {
    private final EventRepository events;private final OrderRepository orders;private final ObjectMapper json;private final String token;
    public FinanceSourceController(EventRepository events,OrderRepository orders,ObjectMapper json,@Value("${app.identity.service-token}")String token){this.events=events;this.orders=orders;this.json=json;this.token=token;}
    @GetMapping("/events/{event}") @Transactional
    public Map<String,Object> snapshot(@PathVariable UUID event,@RequestHeader(value="X-Service-Token",required=false)String supplied){
        service(supplied);var e=events.lock(event);
        var rows=events.db().queryForList("select id,commission_snapshot::text,applied_payment_id from event_commerce.orders where event_id=? order by id",event);
        var values=new ArrayList<Object>();
        for(var row:rows){com.fasterxml.jackson.databind.node.ObjectNode node=json.valueToTree(orders.get((UUID)row.get("id")));
            try{if(row.get("commission_snapshot")!=null)node.set("commissionSnapshot",json.readTree((String)row.get("commission_snapshot")));}catch(Exception ex){throw new IllegalStateException(ex);}
            if(row.get("applied_payment_id")!=null)node.put("appliedPaymentId",row.get("applied_payment_id").toString());
            node.remove(List.of("customerId","terms","serverTime","canPay","canCancel","canConfirmFree"));values.add(node);
        }
        return Map.of("event",e,"orders",values);
    }
    @GetMapping("/organizations/{org}/events")
    public List<Map<String,Object>> ids(@PathVariable UUID org,@RequestHeader(value="X-Service-Token",required=false)String supplied){service(supplied);return events.db().queryForList("select id,name,status from event_commerce.events where organization_id=? order by start_at desc nulls last,id",org);}
    @GetMapping("/reporting-events") @Transactional
    public Map<String,Object> reporting(@RequestParam(required=false)UUID cursor,@RequestHeader(value="X-Service-Token",required=false)String supplied){
        service(supplied);var ids=events.db().queryForList("select id from event_commerce.events where (?::uuid is null or id>?) order by id limit 26",UUID.class,cursor,cursor);
        var data=ids.stream().limit(25).map(id->snapshot(id,supplied)).toList();var result=new LinkedHashMap<String,Object>();result.put("data",data);result.put("nextCursor",ids.size()>25?ids.get(24):null);return result;
    }
    private void service(String value){if(value==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");}
}
