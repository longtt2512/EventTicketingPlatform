package com.eventticketing.commerce.infrastructure;
import com.eventticketing.commerce.api.ApiException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class AttendeeClient {
 private final RestClient client;private final String token;
 public AttendeeClient(@Value("${app.discovery-base-url:http://localhost:8084}") String url,@Value("${app.identity.service-token}") String token){
  var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));client=RestClient.builder().baseUrl(url).requestFactory(factory).build();this.token=token;
 }
 public Map<String,Object> list(UUID event,UUID cursor,int limit){
  try{return client.get().uri(builder->builder.path("/internal/events/{id}/attendees").queryParam("limit",limit).queryParamIfPresent("cursor",Optional.ofNullable(cursor)).build(event)).header("X-Service-Token",token).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>(){});}
  catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ATTENDEES_UNAVAILABLE","Danh sách người tham dự tạm thời chưa tải được. Hãy thử lại.");}
 }
}
