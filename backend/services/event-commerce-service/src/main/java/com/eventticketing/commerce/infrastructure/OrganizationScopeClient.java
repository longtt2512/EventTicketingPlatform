package com.eventticketing.commerce.infrastructure;
import com.eventticketing.commerce.api.ApiException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class OrganizationScopeClient {
    private final RestClient client;private final String token;
    public OrganizationScopeClient(@Value("${app.organization-base-url}") String url,@Value("${app.identity.service-token}") String token) {
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();this.token=token;
    }
    public void require(UUID org,UUID user,boolean mutation) { details(org,user,mutation); }
    public Profile details(UUID org,UUID user,boolean mutation) {
        try {return client.get().uri("/internal/organizations/{org}/authorization?userId={user}&mutation={mutation}",org,user,mutation).header("X-Service-Token",token).retrieve().body(Profile.class);}
        catch(org.springframework.web.client.HttpClientErrorException ex) {throw new ApiException(ex.getStatusCode().value()==409?HttpStatus.CONFLICT:HttpStatus.NOT_FOUND,ex.getStatusCode().value()==409?"ORGANIZATION_SUSPENDED":"RESOURCE_NOT_FOUND","The organization cannot authorize this action.");}
        catch(RuntimeException ex) {throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ORGANIZATION_UNAVAILABLE","The organization service is temporarily unavailable.");}
    }
    public void active(UUID org) {
        try {
            var result=client.get().uri("/internal/organizations/{org}/status",org).header("X-Service-Token",token).retrieve().body(Profile.class);
            if(result==null || !"ACTIVE".equals(result.status()))throw new ApiException(HttpStatus.CONFLICT,"ORGANIZATION_SUSPENDED","Tổ chức đang tạm ngưng. Không thể giữ chỗ mới.");
        }catch(ApiException ex){throw ex;}catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ORGANIZATION_UNAVAILABLE","Không thể xác nhận trạng thái nhà tổ chức. Hãy thử lại.");}
    }
    public record Profile(UUID organizationId,String status,String name,String description) {}
}
