package com.eventticketing.discovery.reporting;

import com.eventticketing.discovery.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReportAccess {
    private final RestClient organization;
    public ReportAccess(@Value("${app.organization-base-url}")String base,@Value("${app.identity.service-token}")String token){
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        organization=RestClient.builder().baseUrl(base).defaultHeader("X-Service-Token",token).requestFactory(factory).build();
    }
    public void require(UUID org,UUID actor){
        if(org==null)return;
        try{JsonNode result=organization.get().uri("/internal/organizations/{org}/finance?userId={actor}",org,actor).retrieve().body(JsonNode.class);if(result==null)throw new IllegalStateException();}
        catch(org.springframework.web.client.HttpClientErrorException ex){throw new ApiException(HttpStatus.FORBIDDEN,"FINANCE_PERMISSION_REQUIRED","Bạn không có quyền xem báo cáo tài chính của tổ chức này.");}
        catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"DEPENDENCY_UNAVAILABLE","Chưa thể kiểm tra quyền báo cáo. Hãy thử lại.");}
    }
}
