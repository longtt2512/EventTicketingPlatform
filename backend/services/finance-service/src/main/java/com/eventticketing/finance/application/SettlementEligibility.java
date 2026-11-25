package com.eventticketing.finance.application;

import com.eventticketing.finance.api.ApiException;
import com.eventticketing.finance.infrastructure.FinanceSources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SettlementEligibility {
    private final FinanceSources sources;private final ObjectMapper json;
    public SettlementEligibility(FinanceSources sources,ObjectMapper json){this.sources=sources;this.json=json;}
    public ObjectNode evaluate(UUID org,UUID event,UUID actor,boolean mutation){
        JsonNode organization=sources.organization(org,actor,mutation),commerce=sources.commerce(event),payment=sources.payment(event);
        var eventState=commerce.path("event");
        if(!org.toString().equals(eventState.path("organizationId").asText()))throw new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Không tìm thấy sự kiện trong tổ chức này.");
        var reasons=new LinkedHashMap<String,String>();
        if(!"COMPLETED".equals(eventState.path("status").asText())||eventState.path("cancellationPending").asBoolean())reasons.put("EVENT_NOT_COMPLETED","Sự kiện chưa hoàn tất hoặc đã hủy.");
        if(!"ACTIVE".equals(organization.path("status").asText()))reasons.put("ORGANIZATION_SUSPENDED","Tổ chức đang bị tạm dừng.");
        if(organization.path("payout").isNull()||organization.path("payout").isMissingNode())reasons.put("PAYOUT_UNVERIFIED","Chưa có tài khoản nhận tiền đã xác minh.");
        if(!payment.path("openExceptions").isArray()||!payment.path("openExceptions").isEmpty())reasons.put("PAYMENT_EXCEPTION_OPEN","Còn nghĩa vụ hoặc khoản thu chưa xác định cần xử lý.");
        for(var attempt:payment.path("attempts"))if("PENDING".equals(attempt.path("status").asText()))reasons.put("PAYMENT_UNKNOWN","Còn giao dịch chưa có kết quả cuối cùng.");
        long gross=0,fees=0,gateway=0,returns=0,reversed=0;boolean known=true;var items=json.createArrayNode();
        try{
            for(var order:commerce.path("orders")){
                if(!"CONFIRMED".equals(order.path("status").asText()))continue;
                long amount=order.path("totalVnd").longValue();gross=Math.addExact(gross,amount);
                var snapshot=order.path("commissionSnapshot");long fee=0;
                if(!snapshot.isObject()||!snapshot.path("platformFeeVnd").canConvertToLong()||snapshot.path("grossVnd").asLong(-1)!=amount){
                    known=false;reasons.put("COMMISSION_SNAPSHOT_MISSING","Có đơn lịch sử chưa có snapshot phí. Không được tự áp phí mới.");
                }else{fee=snapshot.path("platformFeeVnd").longValue();fees=Math.addExact(fees,fee);}
                if(amount>0){
                    String applied=order.path("appliedPaymentId").asText();JsonNode paid=null;
                    for(var transaction:payment.path("transactions"))if(applied.equals(transaction.path("id").asText()))paid=transaction;
                    if(paid==null||!Set.of("SUCCESS","REFUNDED").contains(paid.path("status").asText())||!"APPLIED".equals(paid.path("result").asText())||paid.path("amount").asLong(-1)!=amount||!"VND".equals(paid.path("currency").asText())){
                        known=false;reasons.put("PAYMENT_NOT_RECONCILED","Chưa khớp khoản thu đã áp dụng cho đơn.");
                    }else{
                        if(!paid.path("fee_vnd").canConvertToLong()||paid.path("reconciled_at").isNull()||paid.path("provenance").asText().isBlank()){
                            known=false;reasons.put("GATEWAY_FEE_UNKNOWN","Phí gateway chưa có bằng chứng đối soát.");
                        }else gateway=Math.addExact(gateway,paid.path("fee_vnd").longValue());
                        long returned=0;for(var back:payment.path("returns"))if(applied.equals(back.path("transaction_id").asText()))returned=Math.addExact(returned,back.path("amount").longValue());
                        returns=Math.addExact(returns,returned);
                        if(returned==amount)reversed=Math.addExact(reversed,fee);
                        else if(returned>0){known=false;reasons.put("PARTIAL_RETURN_PENDING","Đơn đang được hoàn từng phần. Phí được hưởng còn chờ xử lý.");}
                    }
                }
                items.addObject().put("orderId",order.path("id").asText()).put("grossVnd",amount).set("commissionSnapshot",snapshot.isMissingNode()?json.nullNode():snapshot);
            }
        }catch(ArithmeticException ex){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"AMOUNT_OVERFLOW","Tổng tiền vượt giới hạn VND. Cần kiểm tra sổ giao dịch.");}
        if(items.isEmpty())reasons.put("NO_CONFIRMED_ORDERS","Chưa có đơn xác nhận để đối soát.");
        long adjustments=Math.subtractExact(returns,reversed);Long net=known?Math.subtractExact(Math.subtractExact(Math.subtractExact(gross,fees),gateway),adjustments):null;
        if(net!=null&&net<0)reasons.put("NEGATIVE_NET","Doanh thu thuần âm. Cần xử lý tài chính trước khi chi trả.");
        ObjectNode result=json.createObjectNode();result.put("eventId",event.toString()).put("organizationId",org.toString()).put("eventName",eventState.path("name").asText())
            .put("eventVersion",eventState.path("version").asLong()).put("currency","VND").put("grossVnd",gross).put("returnedVnd",returns).put("platformFeeReversalVnd",reversed)
            .put("adjustmentsVnd",adjustments).put("eligible",reasons.isEmpty()).put("paymentFingerprint",payment.path("fingerprint").asText()).put("checkedAt",Instant.now().toString());
        if(known){result.put("platformFeeVnd",fees).put("gatewayFeeVnd",gateway).put("netVnd",net);}else{result.putNull("platformFeeVnd");result.putNull("gatewayFeeVnd");result.putNull("netVnd");}
        result.set("payout",organization.path("payout"));result.set("items",items);result.set("reasons",json.valueToTree(reasons));return result;
    }
}
