package com.eventticketing.discovery.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ReportCalculator {
    private final ObjectMapper json;
    public ReportCalculator(ObjectMapper json){this.json=json;}
    public ObjectNode calculate(List<JsonNode> orders,List<JsonNode> events,List<JsonNode> ledgers,List<JsonNode> settlements,
        Instant from,Instant to,ZoneId zone){
        var transactions=new HashMap<String,JsonNode>();var returned=new HashMap<String,Long>();
        long cash=0,refunds=0,attempts=0,successes=0,unknownOutcomes=0,settled=0;var exceptionIds=new HashSet<String>();
        for(var ledger:ledgers){
            for(var t:ledger.path("transactions")){
                transactions.put(t.path("id").asText(),t);
                if(Set.of("SUCCESS","REFUNDED").contains(t.path("status").asText())&&inside(t.path("paid_at"),from,to))cash=Math.addExact(cash,t.path("amount").longValue());
            }
            for(var r:ledger.path("returns")){
                returned.merge(r.path("transaction_id").asText(),r.path("amount").longValue(),Math::addExact);
                if(inside(r.path("returned_at"),from,to))refunds=Math.addExact(refunds,r.path("amount").longValue());
            }
            for(var a:ledger.path("attempts"))if(Set.of("SUCCESS","REFUNDED","FAILED").contains(a.path("status").asText())&&inside(a.path("outcome_at"),from,to)){
                attempts++;if(!"FAILED".equals(a.path("status").asText()))successes++;
            }
            for(var e:ledger.path("openExceptions"))exceptionIds.add(e.path("id").asText());
            for(var a:ledger.path("attempts"))if(Set.of("SUCCESS","REFUNDED","FAILED").contains(a.path("status").asText())&&!a.hasNonNull("outcome_at"))unknownOutcomes++;
        }
        for(var s:settlements)if("SETTLED".equals(s.path("status").asText())&&inside(s.path("completedAt"),from,to))settled=Math.addExact(settled,s.path("snapshot").path("netVnd").longValue());
        long gross=0,fees=0,gateway=0,adjustments=0,tickets=0,paidTickets=0,unknown=0;
        var rows=json.createArrayNode();var daily=new TreeMap<String,long[]>();var types=new TreeMap<String,long[]>();
        for(var order:orders){
            if(!inside(order.path("confirmedAt"),from,to))continue;
            long amount=order.path("totalVnd").longValue();gross=Math.addExact(gross,amount);
            JsonNode snapshot=order.path("commissionSnapshot");boolean known=snapshot.isObject()&&snapshot.path("platformFeeVnd").canConvertToLong()&&snapshot.path("grossVnd").asLong(-1)==amount;
            long fee=known?snapshot.path("platformFeeVnd").longValue():0,gatewayFee=0,adjustment=0,count=0;
            String applied=order.path("appliedPaymentId").asText();
            if(amount>0){var t=transactions.get(applied);
                if(t==null||!t.path("fee_vnd").canConvertToLong()||!t.hasNonNull("reconciled_at")||!"APPLIED".equals(t.path("result").asText())||t.path("amount").asLong(-1)!=amount)known=false;
                else gatewayFee=t.path("fee_vnd").longValue();
                long back=returned.getOrDefault(applied,0L);adjustment=back-(back==amount?fee:0);
                if(back>0&&back<amount)known=false;
            }
            String day=Instant.parse(order.path("confirmedAt").asText()).atZone(zone).toLocalDate().toString();var bucket=daily.computeIfAbsent(day,k->new long[2]);bucket[0]=Math.addExact(bucket[0],amount);
            for(var item:order.path("items")){
                long quantity=item.path("quantity").longValue();count=Math.addExact(count,quantity);if(item.path("unitPrice").longValue()>0)paidTickets=Math.addExact(paidTickets,quantity);
                String label=item.path("name").asText();String typeKey=order.path("eventId").asText()+"|"+item.path("ticketTypeId").asText()+"|"+label;
                var byType=types.computeIfAbsent(typeKey,k->new long[2]);byType[0]=Math.addExact(byType[0],quantity);byType[1]=Math.addExact(byType[1],item.path("lineTotal").longValue());
            }
            tickets=Math.addExact(tickets,count);bucket[1]=Math.addExact(bucket[1],count);
            var row=rows.addObject().put("orderId",order.path("id").asText()).put("eventId",order.path("eventId").asText()).put("eventName",order.path("eventName").asText())
                .put("confirmedAt",order.path("confirmedAt").asText()).put("tickets",count).put("grossVnd",amount).put("financeStatus",known?"KNOWN":"PENDING");
            if(known){fees=Math.addExact(fees,fee);gateway=Math.addExact(gateway,gatewayFee);adjustments=Math.addExact(adjustments,adjustment);
                row.put("platformFeeVnd",fee).put("gatewayFeeVnd",gatewayFee).put("adjustmentsVnd",adjustment).put("netVnd",Math.subtractExact(Math.subtractExact(Math.subtractExact(amount,fee),gatewayFee),adjustment));
            }else{unknown++;for(String field:List.of("platformFeeVnd","gatewayFeeVnd","adjustmentsVnd","netVnd"))row.putNull(field);}
        }
        long quota=0,sold=0,reserved=0,available=0;var eventRows=json.createArrayNode();
        for(var event:events){
            long eventQuota=0,eventSold=0,eventAvailable=0;
            for(var type:event.path("ticketTypes")){eventQuota=Math.addExact(eventQuota,type.path("quota").longValue());eventSold=Math.addExact(eventSold,type.path("sold").longValue());eventAvailable=Math.addExact(eventAvailable,type.path("available").longValue());reserved=Math.addExact(reserved,type.path("reserved").longValue());}
            quota=Math.addExact(quota,eventQuota);sold=Math.addExact(sold,eventSold);available=Math.addExact(available,eventAvailable);
            var row=eventRows.addObject().put("id",event.path("id").asText()).put("name",event.path("name").asText()).put("status",event.path("status").asText()).put("quota",eventQuota).put("sold",eventSold).put("available",eventAvailable);
            row.set("capacity",event.path("capacity"));row.set("sellThroughPercent",json.valueToTree(percent(eventSold,eventQuota)));
        }
        var result=json.createObjectNode();var metrics=result.putObject("metrics");
        metrics.put("events",events.size()).put("ticketsSold",tickets).put("paidTickets",paidTickets).put("freeTickets",tickets-paidTickets)
            .put("grossVnd",gross).put("cashCollectedVnd",cash).put("cashReturnedVnd",refunds).put("netCashVnd",cash-refunds).put("settledVnd",settled)
            .put("unknownFeeOrders",unknown).put("quota",quota).put("soldCurrent",sold).put("reservedCurrent",reserved).put("availableCurrent",available).put("openExceptions",exceptionIds.size()).put("unknownOutcomeAttempts",unknownOutcomes);
        metrics.set("sellThroughPercent",json.valueToTree(percent(sold,quota)));metrics.set("paymentSuccessPercent",json.valueToTree(unknownOutcomes>0?null:percent(successes,attempts)));
        if(unknown==0)metrics.put("platformFeeVnd",fees).put("gatewayFeeVnd",gateway).put("adjustmentsVnd",adjustments).put("netVnd",Math.subtractExact(Math.subtractExact(Math.subtractExact(gross,fees),gateway),adjustments));
        else for(String field:List.of("platformFeeVnd","gatewayFeeVnd","adjustmentsVnd","netVnd"))metrics.putNull(field);
        var series=result.putArray("series");daily.forEach((day,value)->series.addObject().put("date",day).put("grossVnd",value[0]).put("tickets",value[1]));
        var byType=result.putArray("byTicketType");types.forEach((key,value)->{String[] fields=key.split("\\|",3);byType.addObject().put("eventId",fields[0]).put("ticketTypeId",fields[1]).put("name",fields[2]).put("tickets",value[0]).put("grossVnd",value[1]);});
        result.set("rows",rows);result.set("events",eventRows);result.put("noSales",rows.isEmpty());return result;
    }
    private boolean inside(JsonNode value,Instant from,Instant to){if(value.isNull()||value.isMissingNode()||value.asText().isBlank())return false;Instant at=Instant.parse(value.asText());return !at.isBefore(from)&&at.isBefore(to);}
    public static BigDecimal percent(long value,long total){return total==0?null:BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);}
}
