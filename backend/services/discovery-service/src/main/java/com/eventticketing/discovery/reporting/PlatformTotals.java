package com.eventticketing.discovery.reporting;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class PlatformTotals {
 private final Map<String,RestClient> clients=new LinkedHashMap<>();private final String token;
 public PlatformTotals(Environment env){token=env.getRequiredProperty("app.service-token");var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(java.time.Duration.ofSeconds(2));f.setReadTimeout(java.time.Duration.ofSeconds(3));clients.put("users",RestClient.builder().baseUrl(env.getRequiredProperty("app.identity.base-url")).requestFactory(f).build());clients.put("organizations",RestClient.builder().baseUrl(env.getRequiredProperty("app.organization-base-url")).requestFactory(f).build());}
 public Map<String,Object> read(){var result=new LinkedHashMap<String,Object>();for(var source:clients.entrySet())try{var counts=source.getValue().get().uri("/internal/operations/stats").header("X-Service-Token",token).retrieve().body(JsonNode.class);if(counts==null||!counts.path(source.getKey()).canConvertToLong())throw new IllegalStateException();result.put(source.getKey(),counts.path(source.getKey()).longValue());}catch(RuntimeException ex){result.put(source.getKey(),null);}result.put("observedAt",Instant.now().toString());return result;}
}
