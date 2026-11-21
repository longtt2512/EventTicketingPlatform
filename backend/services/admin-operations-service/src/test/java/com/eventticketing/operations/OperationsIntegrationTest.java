package com.eventticketing.operations;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.operations.application.*;
import com.eventticketing.operations.infrastructure.IdentityClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
@SpringBootTest(properties="app.audit-poll-enabled=false") @AutoConfigureMockMvc @Testcontainers
class OperationsIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired SettingsService settings;@Autowired AuditStore audit;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 @MockitoBean IdentityClient identity;@MockitoBean JwtDecoder decoder;
 @Test void approvedSettingsHaveVersionAndAuditWhileUnknownAndInvalidCannotWrite(){
  long version=db.queryForObject("select version from admin_audit.settings where key='reminderLeadHours'",Long.class);UUID actor=UUID.randomUUID();
  settings.change("reminderLeadHours",version,actor,new SettingsService.Change(json.valueToTree(48),"Local reminder change"));
  assertThatThrownBy(()->settings.change("reminderLeadHours",version,actor,new SettingsService.Change(json.valueToTree(24),"Stale client"))).hasMessageContaining("đã đổi");
  for(int invalid:List.of(0,169))assertThatThrownBy(()->settings.change("reminderLeadHours",version+1,actor,new SettingsService.Change(json.valueToTree(invalid),"Invalid range"))).hasMessageContaining("168");
  assertThatThrownBy(()->settings.change("gatewaySecret",0,actor,new SettingsService.Change(json.valueToTree("secret"),"Unknown key"))).hasMessageContaining("danh sách");
  assertThat(db.queryForObject("select count(*) from admin_audit.audit_records where actor_id=?",Integer.class,actor)).isEqualTo(1);
 }
 @Test void invalidSupportEmailIsRejectedAndBlankIsAllowed(){long v=db.queryForObject("select version from admin_audit.settings where key='supportEmail'",Long.class);UUID actor=UUID.randomUUID();assertThatThrownBy(()->settings.change("supportEmail",v,actor,new SettingsService.Change(json.valueToTree("bad email"),"Invalid email"))).hasMessageContaining("hợp lệ");settings.change("supportEmail",v,actor,new SettingsService.Change(json.valueToTree(""),"Clear support"));}
 @Test void auditDeduplicatesRedactsAndKeepsImmutableCanonicalCopy(){UUID id=UUID.randomUUID();var row=json.createObjectNode().put("id",id.toString()).put("actor_id",UUID.randomUUID().toString()).put("resource_id","event").put("action","TEST_MODERATION").put("occurred_at",Instant.now().toString()).put("reason","Account 123456789012");row.putObject("after_value").put("accessToken","private-token").put("accountNumber","123456789012").put("status","ACTIVE");var page=json.createObjectNode();page.putArray("data").add(row);audit.ingest("test",page);row.put("action","CHANGED");audit.ingest("test",page);var stored=json.valueToTree(audit.detail("test",id));assertThat(stored.toString()).doesNotContain("private-token","123456789012","CHANGED").contains("ACTIVE","TEST_MODERATION");assertThat(db.queryForObject("select count(*) from admin_audit.audit_records where source='test' and id=?",Integer.class,id)).isEqualTo(1);}
 @Test void auditPaginationAndBadFiltersAreDeterministic(){var page=audit.list(null,null,null,null,null,null,1);assertThat(page).containsKeys("sources","hasMore");assertThatThrownBy(()->audit.list(null,null,null,null,null,"malformed",20)).hasMessageContaining("không hợp lệ");}
 @Test void publicNonAdminAndWrongServiceTokenCannotAccessOperations()throws Exception{mvc.perform(get("/api/v1/admin/settings")).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/admin/audit-logs").with(jwt())).andExpect(status().isForbidden());mvc.perform(get("/internal/operations/settings").header("X-Service-Token","wrong")).andExpect(status().isForbidden());mvc.perform(get("/internal/operations/settings").header("X-Service-Token","local-service-token")).andExpect(status().isOk()).andExpect(jsonPath("$.fixedPolicies.reservationMinutes").value(10));}
}
