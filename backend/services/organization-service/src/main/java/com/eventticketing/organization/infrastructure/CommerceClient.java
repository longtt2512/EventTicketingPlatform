package com.eventticketing.organization.infrastructure;
import com.eventticketing.organization.api.ApiException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class CommerceClient {
    private final RestClient client;private final String token;
    public CommerceClient(@Value("${app.commerce-base-url}") String url,@Value("${app.identity.service-token}") String token) {
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        this.client=RestClient.builder().baseUrl(url).requestFactory(factory).build();this.token=token;
    }
    public Event event(UUID id) {
        try {return client.get().uri("/internal/events/{id}",id).header("X-Service-Token",token).retrieve().body(Event.class);}
        catch(org.springframework.web.client.HttpClientErrorException ex) {throw new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Event was not found.");}
        catch(RuntimeException ex) {throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"EVENT_SERVICE_UNAVAILABLE","The event service is temporarily unavailable.");}
    }
    public java.util.List<UUID> events(UUID org){try{var ids=client.get().uri("/internal/organizations/{org}/event-ids",org).header("X-Service-Token",token).retrieve().body(new org.springframework.core.ParameterizedTypeReference<java.util.List<UUID>>(){});return ids==null?java.util.List.of():ids;}catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"EVENT_SERVICE_UNAVAILABLE","The event service is temporarily unavailable.");}}
    public record Event(UUID id,UUID organizationId,String name,String status) {}
}
