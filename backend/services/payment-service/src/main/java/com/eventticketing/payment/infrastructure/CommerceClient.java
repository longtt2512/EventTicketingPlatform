package com.eventticketing.payment.infrastructure;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.eventticketing.payment.api.ApiException;
import org.springframework.http.HttpStatus;
@Component
public class CommerceClient {
 private final RestClient client;private final String token;
 public CommerceClient(@Value("${app.commerce-base-url}")String base,@Value("${app.identity.service-token}")String token){var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(5));client=RestClient.builder().baseUrl(base).requestFactory(factory).build();this.token=token;}
 public Snapshot read(UUID id){try{return client.get().uri("/internal/orders/{id}",id).header("X-Service-Token",token).retrieve().body(Snapshot.class);}catch(org.springframework.web.client.HttpClientErrorException e){throw new ApiException(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND","Không tìm thấy đơn hàng.");}catch(Exception e){throw unavailable();}}
 public Claim claim(UUID id,UUID actor,String key){try{return client.post().uri("/internal/orders/{id}/payment-claims",id).header("X-Service-Token",token).body(Map.of("customerId",actor,"requestKey",key)).retrieve().body(Claim.class);}catch(org.springframework.web.client.HttpClientErrorException e){throw new ApiException(e.getStatusCode().value()==403?HttpStatus.FORBIDDEN:HttpStatus.CONFLICT,"PAYMENT_UNRESOLVED","Không thể tạo thanh toán mới. Kiểm tra trạng thái đơn, xác thực email hoặc tiếp tục giao dịch đang chờ.");}catch(Exception e){throw unavailable();}}
 private ApiException unavailable(){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"COMMERCE_UNAVAILABLE","Chưa kiểm tra được đơn hàng. Hãy thử kiểm tra trạng thái sau. Không tạo giao dịch mới.");}
 public record Snapshot(UUID id,UUID customerId,UUID eventId,UUID organizationId,String eventName,String status,String eventStatus,long totalVnd,String currency,Instant expiresAt,String paymentStatus){}
 public record Claim(UUID attemptId,Snapshot order){}
}
