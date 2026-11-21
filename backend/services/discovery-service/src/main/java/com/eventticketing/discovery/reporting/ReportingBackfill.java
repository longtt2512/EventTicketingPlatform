package com.eventticketing.discovery.reporting;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
@Service @ConditionalOnProperty(name="app.reporting-backfill-enabled",havingValue="true",matchIfMissing=true)
public class ReportingBackfill {
 private final JdbcTemplate db;private final RestClient commerce;private final String token;private final ReportingProjection projection;
 public ReportingBackfill(JdbcTemplate db,ReportingProjection projection,@Value("${app.commerce-base-url}")String base,@Value("${app.service-token}")String token){this.db=db;this.projection=projection;this.token=token;var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(java.time.Duration.ofSeconds(2));f.setReadTimeout(java.time.Duration.ofSeconds(5));commerce=RestClient.builder().baseUrl(base).requestFactory(f).build();}
 @Scheduled(fixedDelayString="${app.reporting-backfill-millis:5000}",initialDelayString="${app.reporting-backfill-millis:5000}") public void sync(){try{
  UUID cursor=db.queryForObject("select cursor from discovery.reporting_sources where source='commerce'",UUID.class);
  var page=commerce.get().uri("/internal/finance/reporting-events"+(cursor==null?"":"?cursor="+cursor)).header("X-Service-Token",token).retrieve().body(JsonNode.class);if(page==null||!page.path("data").isArray())throw new IllegalStateException("Missing page");
  for(var row:page.path("data")){var e=(ObjectNode)row.path("event").deepCopy();UUID event=UUID.fromString(e.path("id").asText()),org=UUID.fromString(e.path("organizationId").asText());e.remove(List.of("reviewReason","terms","description","organizationDescription"));projection.upsert("EVENT",event,org,event,e.path("version").longValue(),e);
   for(var node:row.path("orders"))if("CONFIRMED".equals(node.path("status").asText())){var order=(ObjectNode)node.deepCopy();order.remove(List.of("customerId","terms","serverTime","canPay","canCancel","canConfirmFree"));projection.upsert("ORDER",UUID.fromString(order.path("id").asText()),org,event,order.path("version").longValue(),order);}
  }
  UUID next=page.hasNonNull("nextCursor")?UUID.fromString(page.path("nextCursor").asText()):null;
  db.update("update discovery.reporting_sources set cursor=?,synchronized_at=clock_timestamp(),completed_at=case when ?::uuid is null then clock_timestamp() else completed_at end,last_error=null where source='commerce'",next,next);
 }catch(RuntimeException ex){db.update("update discovery.reporting_sources set last_error='Source unavailable; retry pending' where source='commerce'");}}
}
