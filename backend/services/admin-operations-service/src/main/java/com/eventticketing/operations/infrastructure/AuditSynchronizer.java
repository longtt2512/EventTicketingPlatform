package com.eventticketing.operations.infrastructure;

import com.eventticketing.operations.application.AuditStore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Repeated full, paginated scans also recover rows committed behind a previous cursor. */
@Component @ConditionalOnProperty(name="app.audit-poll-enabled",havingValue="true",matchIfMissing=true)
public class AuditSynchronizer {
    private final AuditStore store;private final Map<String,RestClient> sources=new LinkedHashMap<>();
    public AuditSynchronizer(AuditStore store,@Value("${app.identity.base-url}")String identity,@Value("${app.organization-base-url}")String organization,
        @Value("${app.commerce-base-url}")String commerce,@Value("${app.payment-base-url}")String payment,@Value("${app.finance-base-url}")String finance,
        @Value("${app.discovery-base-url}")String discovery,@Value("${app.identity.service-token}")String token){
        this.store=store;var urls=Map.of("identity",identity,"organization",organization,"commerce",commerce,"payment",payment,"finance",finance,"reporting",discovery);
        urls.forEach((name,url)->{var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(java.time.Duration.ofSeconds(1));factory.setReadTimeout(java.time.Duration.ofSeconds(3));sources.put(name,RestClient.builder().baseUrl(url).defaultHeader("X-Service-Token",token).requestFactory(factory).build());});
    }
    @Scheduled(fixedDelayString="${app.audit-poll-ms:5000}",initialDelayString="${app.audit-poll-ms:5000}")
    public void run(){for(var entry:sources.entrySet())try{
        UUID cursor=store.cursor(entry.getKey());var page=entry.getValue().get().uri("/internal/operations/audit"+(cursor==null?"":"?cursor="+cursor)).retrieve().body(JsonNode.class);
        if(page==null)throw new IllegalStateException();store.ingest(entry.getKey(),page);
    }catch(RuntimeException ex){store.failed(entry.getKey());}}
}
