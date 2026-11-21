package com.eventticketing.notification;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class OperationsSources {
 private final Map<String,RestClient> clients=new LinkedHashMap<>();private final String token;
 public OperationsSources(Environment env){token=env.getRequiredProperty("app.service-token");for(String source:List.of("organization","commerce","finance","operations","admission")){var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(2));factory.setReadTimeout(java.time.Duration.ofSeconds(3));clients.put(source,RestClient.builder().baseUrl(env.getRequiredProperty("app."+source+"-base-url")).requestFactory(factory).build());}}
 public JsonNode read(String source,String path){return clients.get(source).get().uri(path).header("X-Service-Token",token).retrieve().body(JsonNode.class);}
 public void ack(String source,UUID id){clients.get(source).post().uri("/internal/notifications/{id}/ack",id).header("X-Service-Token",token).retrieve().toBodilessEntity();}
 public int hours(){for(var setting:read("operations","/internal/operations/settings").path("settings"))if("reminderLeadHours".equals(setting.path("key").asText()))return setting.path("value").intValue();throw new IllegalStateException("Reminder policy unavailable");}
 public boolean reminderEligible(JsonNode p){String query="?eventId="+p.path("eventId").asText()+"&userId="+p.path("userId").asText();return read("commerce","/internal/notifications/reminder-check"+query+"&hours="+hours()).path("eligible").asBoolean()&&read("admission","/internal/notifications/reminder-check"+query).path("eligible").asBoolean();}
}
