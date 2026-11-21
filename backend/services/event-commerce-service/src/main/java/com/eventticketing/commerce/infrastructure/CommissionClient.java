package com.eventticketing.commerce.infrastructure;

import com.eventticketing.commerce.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CommissionClient {
    private final RestClient client;
    private final String token;
    public CommissionClient(@Value("${app.finance-base-url}")String baseUrl,
                            @Value("${app.identity.service-token}")String token) {
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();this.token=token;
    }
    public JsonNode quote(UUID order,List<Map<String,Object>> lines) {
        try {
            var snapshot=client.post().uri("/internal/commission-quotes").header("X-Service-Token",token)
                .body(Map.of("orderId",order,"lines",lines)).retrieve().body(JsonNode.class);
            if(snapshot==null||!snapshot.hasNonNull("configId")||!snapshot.hasNonNull("capturedAt")
                ||!snapshot.path("grossVnd").canConvertToLong()||!snapshot.path("platformFeeVnd").canConvertToLong())
                throw new IllegalStateException("Incomplete commission quote");
            return snapshot;
        } catch(RuntimeException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"COMMISSION_UNAVAILABLE",
                "Chưa xác định được phí của đơn. Vui lòng thử lại; vé chưa bị giữ.");
        }
    }
}
