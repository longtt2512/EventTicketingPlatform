package com.eventticketing.admission.infrastructure;
import static com.eventticketing.admission.application.TicketModels.*;
import com.eventticketing.admission.api.ApiException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class SourceClient {
 private final RestClient commerce,organization,identity;
 public SourceClient(@Value("${app.commerce-base-url}")String commerce,@Value("${app.organization-base-url}")String organization,@Value("${app.identity.base-url}")String identity,@Value("${app.identity.service-token}")String token){this.commerce=client(commerce,token);this.organization=client(organization,token);this.identity=client(identity,token);}
 private RestClient client(String base,String token){var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(java.time.Duration.ofSeconds(2));f.setReadTimeout(java.time.Duration.ofSeconds(3));return RestClient.builder().baseUrl(base).defaultHeader("X-Service-Token",token).requestFactory(f).build();}
 public Order order(UUID id){return call(()->commerce.get().uri("/internal/orders/{id}",id).retrieve().body(Order.class));}
 public Event event(UUID id){return call(()->commerce.get().uri("/internal/events/{id}/fulfillment",id).retrieve().body(Event.class));}
 public Contact contact(UUID id){return call(()->identity.get().uri("/internal/users/{id}/notification-contact",id).retrieve().body(Contact.class));}
 public void authorize(UUID event,UUID actor){call(()->organization.get().uri("/internal/organizations/admission/events/{event}/authorization?userId={actor}",event,actor).retrieve().body(Map.class));}
 public List<UUID> assigned(UUID actor){return call(()->organization.get().uri("/internal/organizations/admission/events?userId={actor}",actor).retrieve().body(new ParameterizedTypeReference<List<UUID>>(){}));}
 private <T>T call(java.util.function.Supplier<T> action){try{T result=action.get();if(result==null)throw new IllegalStateException();return result;}catch(org.springframework.web.client.HttpClientErrorException ex){throw new ApiException(ex.getStatusCode().value()==403?HttpStatus.FORBIDDEN:HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","Không tìm thấy dữ liệu hoặc bạn không còn quyền truy cập.");}catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SOURCE_UNAVAILABLE","Chưa thể xác minh dữ liệu hiện tại. Hãy thử lại khi có kết nối.");}}
}
