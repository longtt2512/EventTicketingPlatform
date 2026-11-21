package com.eventticketing.commerce.infrastructure;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class AdmissionFenceClient {
 private final RestClient client;
 public AdmissionFenceClient(@Value("${app.admission-base-url:http://localhost:8088}")String url,@Value("${app.identity.service-token}")String token){var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(java.time.Duration.ofSeconds(2));f.setReadTimeout(java.time.Duration.ofSeconds(5));client=RestClient.builder().baseUrl(url).requestFactory(f).defaultHeader("X-Service-Token",token).build();}
 public void freeze(UUID event){var result=client.post().uri("/internal/admission/events/{id}/freeze",event).retrieve().body(Acknowledgment.class);if(result==null||!event.equals(result.eventId())||!result.frozen())throw new IllegalStateException("Admission fence not acknowledged");}
 public record Acknowledgment(UUID eventId,boolean frozen){}
}
