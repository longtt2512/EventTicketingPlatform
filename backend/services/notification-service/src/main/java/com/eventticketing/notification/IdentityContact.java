package com.eventticketing.notification;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class IdentityContact {
 private final RestClient client;private final String token;
 public IdentityContact(@Value("${app.identity-base-url}")String base,@Value("${app.service-token}")String token){var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));client=RestClient.builder().baseUrl(base).requestFactory(factory).build();this.token=token;}
 public Contact read(UUID id){var c=client.get().uri("/internal/users/{id}/notification-contact",id).header("X-Service-Token",token).retrieve().body(Contact.class);if(c==null)throw new IllegalStateException("No notification contact");return c;}
 public record Contact(UUID userId,String email,String displayName,String locale){public Contact(UUID userId,String email,String displayName){this(userId,email,displayName,"vi");}}
}
