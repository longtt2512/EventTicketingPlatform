package com.eventticketing.finance.infrastructure;

import com.eventticketing.finance.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinanceSources {
    private final RestClient commerce,payment,organization;
    public FinanceSources(@Value("${app.commerce-base-url}")String commerce,@Value("${app.payment-base-url}")String payment,
        @Value("${app.organization-base-url}")String organization,@Value("${app.identity.service-token}")String token){
        this.commerce=client(commerce,token);this.payment=client(payment,token);this.organization=client(organization,token);
    }
    private RestClient client(String base,String token){var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(java.time.Duration.ofSeconds(2));f.setReadTimeout(java.time.Duration.ofSeconds(5));return RestClient.builder().baseUrl(base).defaultHeader("X-Service-Token",token).requestFactory(f).build();}
    public JsonNode commerce(UUID event){return call(()->commerce.get().uri("/internal/finance/events/{event}",event).retrieve().body(JsonNode.class));}
    public JsonNode payment(UUID event){return call(()->payment.get().uri("/internal/finance/events/{event}",event).retrieve().body(JsonNode.class));}
    public JsonNode organization(UUID org,UUID actor,boolean mutation){return call(()->organization.get().uri("/internal/organizations/{org}/finance?userId={actor}&mutation={mutation}",org,actor,mutation).retrieve().body(JsonNode.class));}
    public JsonNode events(UUID org){return call(()->commerce.get().uri("/internal/finance/organizations/{org}/events",org).retrieve().body(JsonNode.class));}
    public JsonNode authorize(Object request){return call(()->payment.post().uri("/internal/finance/payout-authorizations").body(request).retrieve().body(JsonNode.class));}
    private <T>T call(java.util.function.Supplier<T> action){
        try{T result=action.get();if(result==null)throw new IllegalStateException();return result;}
        catch(org.springframework.web.client.HttpClientErrorException ex){
            if(ex.getStatusCode().value()==403)throw new ApiException(HttpStatus.FORBIDDEN,"FINANCE_PERMISSION_REQUIRED","Bạn không có quyền tài chính hiện hành.");
            if(ex.getStatusCode().value()==404)throw new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Không tìm thấy dữ liệu trong phạm vi của bạn.");
            throw new ApiException(HttpStatus.CONFLICT,"FINANCIAL_STATE_CHANGED","Quyền, tài khoản hoặc nghĩa vụ tài chính đã đổi. Tải lại để kiểm tra.");
        }catch(ApiException ex){throw ex;}
        catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"DEPENDENCY_UNAVAILABLE","Chưa thể xác minh dữ liệu tài chính. Giữ nguyên yêu cầu và thử lại.");}
    }
}
