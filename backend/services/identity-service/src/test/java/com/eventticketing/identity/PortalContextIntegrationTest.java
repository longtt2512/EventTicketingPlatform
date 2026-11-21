package com.eventticketing.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.eventticketing.platform.SecretCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties="app.mail.poll-millis=3600000")
@AutoConfigureMockMvc
@Testcontainers
class PortalContextIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);r.add("app.data-encryption-key",()->Base64.getEncoder().encodeToString(new byte[32]));r.add("app.web-base-url",()->"http://localhost:14200");}
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate db; @Autowired SecretCipher cipher;
 private String lastMail(String email){return db.query("select id,encrypted_body from identity.email_deliveries where recipient=? order by created_at desc",(rs,n)->cipher.decrypt(rs.getString(2),rs.getObject(1).toString()),email).getFirst();}
 @Test void eachPortalSurvivesRegistrationResendResetWithoutGrantingPrivileges() throws Exception {
  for(var entry:Map.of("customer","","organizer","/org","admin","/admin","checkin","/staff").entrySet()){
   String email="portal-"+UUID.randomUUID()+"@example.test";
   var context=Map.of("portal",entry.getKey(),"returnUrl","/home");
   mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password","Portal-test-123!","displayName","Portal test","context",context)))).andExpect(status().isAccepted());
   assertThat(lastMail(email)).contains("http://localhost:14200"+entry.getValue()+"/verify-email?returnUrl=%2Fhome#token=");
   assertThat(db.queryForList("select role from identity.user_roles where user_id=(select id from identity.users where email=?)",String.class,email)).containsExactly("CUSTOMER");
   db.update("update identity.verification_tokens set created_at=now()-interval '61 seconds' where user_id=(select id from identity.users where email=?)",email);
   mvc.perform(post("/api/v1/auth/resend-verification").contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"context",context)))).andExpect(status().isAccepted());
   String verification=lastMail(email);assertThat(verification).contains(entry.getValue()+"/verify-email?returnUrl=%2Fhome#token=");
   String token=verification.split("#token=")[1];
   mvc.perform(post("/api/v1/auth/verify-email").contentType("application/json").content(json.writeValueAsString(Map.of("token",token)))).andExpect(status().isNoContent());
   mvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"context",context)))).andExpect(status().isAccepted());
   assertThat(lastMail(email)).contains(entry.getValue()+"/reset-password?returnUrl=%2Fhome#token=");
  }
 }
 @Test void invalidPortalAndExternalReturnDestinationsAreRejected() throws Exception {
  for(var context:List.of(Map.of("portal","https://evil.test"),Map.of("portal","organizer","returnUrl","//evil.test"),Map.of("returnUrl","https://evil.test"),Map.of("returnUrl","/login"),Map.of("returnUrl","/home\\evil"))){
   mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(json.writeValueAsString(Map.of("email","invalid-"+UUID.randomUUID()+"@example.test","password","Portal-test-123!","displayName","Invalid context","context",context)))).andExpect(status().isBadRequest());
  }
 }
}
