package com.eventticketing.commerce;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.commerce.infrastructure.*;
import com.eventticketing.commerce.api.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
@SpringBootTest(properties={"app.cancellation-delay-ms=3600000","app.messaging.enabled=false","spring.kafka.admin.auto-create=false"}) @AutoConfigureMockMvc @Testcontainers
class DraftEventIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean com.eventticketing.commerce.infrastructure.AdmissionFenceClient admissionFence;
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;@MockitoBean OrganizationScopeClient scope;@MockitoBean IdentityClient identity;@MockitoBean JwtDecoder decoder;
 @Test void allowedDraftPersistsOnlyInOwningServiceAndRejectsForeignOrganization() throws Exception {
  UUID org=UUID.randomUUID(),actor=UUID.randomUUID();
  mvc.perform(post("/api/v1/organizations/"+org+"/events").with(jwt().jwt(j->j.subject(actor.toString()))).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Draft test\"}"))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.organizationId").value(org.toString())).andExpect(jsonPath("$.status").value("DRAFT"));
  verify(scope).require(org,actor,true);
  mvc.perform(get("/api/v1/organizations/"+org+"/events").with(jwt().jwt(j->j.subject(actor.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Draft test"));
  doThrow(new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","No access")).when(scope).require(any(),eq(actor),eq(true));
  mvc.perform(post("/api/v1/organizations/"+UUID.randomUUID()+"/events").with(jwt().jwt(j->j.subject(actor.toString()))).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Foreign draft\"}"))
    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
 }
 @Test void internalLookupRequiresServiceAuthentication() throws Exception {
  mvc.perform(get("/internal/events/"+UUID.randomUUID())).andExpect(status().isUnauthorized());
 }
}
