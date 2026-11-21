package com.eventticketing.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

import com.eventticketing.identity.application.EmailNotificationService;
import com.eventticketing.identity.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "app.mail.poll-millis=3600000")
@AutoConfigureMockMvc
@Testcontainers
class IdentityIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("app.data-encryption-key", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired com.nimbusds.jose.jwk.RSAKey signingKey;
    @MockitoBean EmailNotificationService emails;
    final String password = "Test-only-password-123!";

    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    private MvcResult resend(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",email))))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.retryAfterSeconds").value(60))
            .andExpect(header().string("Cache-Control","no-store")).andReturn();
    }
    private void ageVerification(String email) {
        db.update("update identity.verification_tokens set created_at=now()-interval '61 seconds' where user_id=(select id from identity.users where email=?)",email);
    }
    @Test void resendIsGenericAndIncludesTheInitialRegistrationCooldown() throws Exception {
        String email=register(false);String expected=resend(email).getResponse().getContentAsString();
        assertThat(resend("absent-"+UUID.randomUUID()+"@example.test").getResponse().getContentAsString()).isEqualTo(expected);
        verify(emails,times(1)).sendVerification(eq(email),anyString());
        ageVerification(email);resend(email.toUpperCase(java.util.Locale.ROOT));
        ArgumentCaptor<String> token=ArgumentCaptor.forClass(String.class);verify(emails,times(2)).sendVerification(eq(email),token.capture());
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("token",token.getValue())))).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("token",token.getValue())))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"));
        ageVerification(email);assertThat(resend(email).getResponse().getContentAsString()).isEqualTo(expected);
        verify(emails,times(2)).sendVerification(eq(email),anyString());
    }
    @Test void simultaneousResendsCommitOnlyOneNewVerificationEmail() throws Exception {
        String email=register(false);ageVerification(email);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var gate=new java.util.concurrent.CountDownLatch(1);var tasks=new java.util.ArrayList<java.util.concurrent.Future<MvcResult>>();
            for(int i=0;i<6;i++)tasks.add(pool.submit(()->{gate.await();return resend(email);}));
            gate.countDown();for(var task:tasks)task.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
        verify(emails,times(2)).sendVerification(eq(email),anyString());
        assertThat(db.queryForObject("select count(*) from identity.verification_tokens where user_id=(select id from identity.users where email=?)",Integer.class,email)).isEqualTo(2);
    }
    @Test void lockedAndMalformedResendRequestsDoNotIssueTokens() throws Exception {
        String email=register(false);ageVerification(email);db.update("update identity.users set status='LOCKED' where email=?",email);
        resend(email);verify(emails,times(1)).sendVerification(eq(email),anyString());
        mvc.perform(post("/api/v1/auth/resend-verification").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
    }

    private String register(boolean verify) throws Exception {
        String email = "identity-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password, "displayName", "Test account", "locale", "vi"))))
                .andExpect(status().isAccepted());
        if (verify) {
            ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
            verify(emails).sendVerification(eq(email), token.capture());
            mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("token", token.getValue())))).andExpect(status().isNoContent());
        }
        return email;
    }
    private MvcResult login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
    }
    private String bearer(MvcResult result) throws Exception {
        return "Bearer " + json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
    private Cookie cookie(MvcResult result) {
        String value = result.getResponse().getHeader("Set-Cookie").split(";", 2)[0].split("=", 2)[1];
        return new Cookie("refresh_token", value);
    }

    @Test void verifiedUserCanUpdateOwnProfileButCannotGrantRoles() throws Exception {
        var session = login(register(true));
        mvc.perform(patch("/api/v1/users/me").header("Authorization", bearer(session)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Updated name\",\"preferredLocale\":\"en\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Updated name"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(session))).andExpect(status().isForbidden());
    }

    @Test void cookieEndpointsRejectMissingAndUntrustedOrigin() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORIGIN_NOT_ALLOWED"));
        mvc.perform(post("/api/v1/auth/logout").header("Origin", "https://attacker.example")).andExpect(status().isForbidden());
    }

    @Test void logoutRevokesTheAlreadyIssuedAccessToken() throws Exception {
        var session = login(register(true));
        mvc.perform(post("/api/v1/auth/logout").header("Origin", "http://localhost:14200").cookie(cookie(session))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(session)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test void replayRevocationSurvivesTheErrorTransaction() throws Exception {
        var initial = login(register(true));
        var rotated = mvc.perform(post("/api/v1/auth/refresh").header("Origin", "http://localhost:14200").cookie(cookie(initial))).andExpect(status().isOk()).andReturn();
        mvc.perform(post("/api/v1/auth/refresh").header("Origin", "http://localhost:14200").cookie(cookie(initial)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("REFRESH_REUSE_DETECTED"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(rotated))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").header("Origin", "http://localhost:14200").cookie(cookie(rotated))).andExpect(status().isUnauthorized());
    }

    @Test void failedLoginCountSurvivesAuthenticationFailure() throws Exception {
        String email = register(true);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "incorrect"))))
                .andExpect(status().isUnauthorized());
        assertThat(users.findByEmail(email).orElseThrow().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test void lockedAccountCannotUseExistingJwt() throws Exception {
        String email = register(true);
        var session = login(email);
        var user = users.findByEmail(email).orElseThrow(); user.lock(); users.save(user);
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(session))).andExpect(status().isUnauthorized());
    }

    @Test void verificationIsSingleUseAndWrongPasswordDoesNotDisclosePendingAccount() throws Exception {
        String email = register(false);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "incorrect"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emails).sendVerification(eq(email), token.capture());
        String body = json.writeValueAsString(Map.of("token", token.getValue()));
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity());
    }

    @Test void passwordResetDoesNotUnlockAnAdministrativelyLockedAccount() throws Exception {
        String email = register(true);
        var user = users.findByEmail(email).orElseThrow(); user.lock(); users.save(user);
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email)))).andExpect(status().isAccepted());
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emails).sendPasswordReset(eq(email), token.capture());
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token.getValue(), "newPassword", "Another-password-123!"))))
                .andExpect(status().isNoContent());
        assertThat(users.findByEmail(email).orElseThrow().getStatus().name()).isEqualTo("LOCKED");
    }

    @Test void registrationDoesNotRevealWhetherTheEmailExists() throws Exception {
        String email = register(false);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email.toUpperCase(java.util.Locale.ROOT), "password", password, "displayName", "Second"))))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
        verify(emails, times(1)).sendVerification(eq(email), anyString());
        assertThat(users.findByEmail(email).orElseThrow().getDisplayName()).isEqualTo("Test account");
    }

    @Test void jwtValidationRejectsInvalidIssuerAudienceLifetimeSubjectAndSignature() throws Exception {
        var session = login(register(true));
        var original = com.nimbusds.jwt.SignedJWT.parse(bearer(session).substring(7));
        java.util.List<java.util.function.UnaryOperator<com.nimbusds.jwt.JWTClaimsSet.Builder>> variants = java.util.List.of(
                claims -> claims.issuer("https://untrusted.example"),
                claims -> claims.audience("different-api"),
                claims -> claims.audience((String) null),
                claims -> claims.expirationTime(java.util.Date.from(java.time.Instant.now().minusSeconds(120))),
                claims -> claims.expirationTime(null),
                claims -> claims.notBeforeTime(java.util.Date.from(java.time.Instant.now().plusSeconds(120))),
                claims -> claims.subject("not-a-uuid"),
                claims -> claims.claim("sid", null));
        for (var variant : variants) {
            var forged = new com.nimbusds.jwt.SignedJWT(original.getHeader(), variant.apply(new com.nimbusds.jwt.JWTClaimsSet.Builder(original.getJWTClaimsSet())).build());
            forged.sign(new com.nimbusds.jose.crypto.RSASSASigner(signingKey));
            mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + forged.serialize())).andExpect(status().isUnauthorized());
        }
        var unknownKey = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID("unknown-key").generate();
        var forged = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256).keyID("unknown-key").build(), original.getJWTClaimsSet());
        forged.sign(new com.nimbusds.jose.crypto.RSASSASigner(unknownKey));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + forged.serialize())).andExpect(status().isUnauthorized());
        var unsigned = new com.nimbusds.jwt.PlainJWT(original.getJWTClaimsSet());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + unsigned.serialize())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(session))).andExpect(status().isOk());
    }

    @Test void staleAdminClaimDoesNotOverrideCurrentAccountRoles() throws Exception {
        var original = com.nimbusds.jwt.SignedJWT.parse(bearer(login(register(true))).substring(7));
        var stale = new com.nimbusds.jwt.SignedJWT(original.getHeader(), new com.nimbusds.jwt.JWTClaimsSet.Builder(original.getJWTClaimsSet()).claim("roles", java.util.List.of("ADMIN")).build());
        stale.sign(new com.nimbusds.jose.crypto.RSASSASigner(signingKey));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + stale.serialize())).andExpect(status().isForbidden());
    }

    @Test void concurrentRefreshConsumesOnceAndReplayRevokesTheWinningSession() throws Exception {
        var initial = login(register(true));
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<MvcResult> refresh = () -> { start.await(); return mvc.perform(post("/api/v1/auth/refresh").header("Origin", "http://localhost:14200").cookie(cookie(initial))).andReturn(); };
            var first = executor.submit(refresh); var second = executor.submit(refresh); start.countDown();
            var results = java.util.List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS), second.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(results.stream().map(result -> result.getResponse().getStatus()).sorted().toList()).containsExactly(200, 401);
            var winner = results.stream().filter(result -> result.getResponse().getStatus() == 200).findFirst().orElseThrow();
            mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(winner))).andExpect(status().isUnauthorized());
        }
    }

    @Test void fiveFailuresTemporarilyLockForFifteenMinutesThenPermitLogin() throws Exception {
        String email=register(true);var original=login(email);
        for(int i=0;i<5;i++) mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",email,"password","incorrect")))).andExpect(status().isUnauthorized());
        var user=users.findByEmail(email).orElseThrow();
        assertThat(user.getLockedUntil()).isAfter(java.time.Instant.now().plusSeconds(14*60)).isBefore(java.time.Instant.now().plusSeconds(16*60));
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        mvc.perform(get("/api/v1/users/me").header("Authorization",bearer(original))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",email,"password",password)))).andExpect(status().isUnauthorized());
        user.recordFailedLogin(java.time.Instant.now().minusSeconds(1));users.save(user);
        login(email);
        assertThat(users.findByEmail(email).orElseThrow().getStatus().name()).isEqualTo("ACTIVE");
        mvc.perform(get("/api/v1/users/me").header("Authorization",bearer(original))).andExpect(status().isUnauthorized());
    }
    @Test void minimumTenCharacterPasswordIsAcceptedAndShorterIsRejected() throws Exception {
        for(String pass:java.util.List.of("123456789","1234567890")) {
            mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",UUID.randomUUID()+"@example.test","password",pass,"displayName","Policy test"))))
                .andExpect(status().is(pass.length()==10?202:400));
        }
    }

    @Test void internalRoleGrantRequiresServiceCredentialAndRejectsAdminRole() throws Exception {
        String email=register(true);UUID id=users.findByEmail(email).orElseThrow().getId();
        mvc.perform(post("/internal/users/"+id+"/roles").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ORGANIZER\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SERVICE_AUTH_REQUIRED"));
        mvc.perform(post("/internal/users/"+id+"/roles").header("X-Service-Token","local-service-token").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ROLE"));
        mvc.perform(post("/internal/users/"+id+"/roles").header("X-Service-Token","local-service-token").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ORGANIZER\"}"))
            .andExpect(status().isOk());
        assertThat(users.findById(id).orElseThrow().getRoles()).contains("ORGANIZER").doesNotContain("ADMIN");
    }
}
