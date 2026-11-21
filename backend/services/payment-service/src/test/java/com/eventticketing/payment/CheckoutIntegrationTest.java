package com.eventticketing.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.payment.application.*;
import com.eventticketing.payment.application.PaymentModels.*;
import com.eventticketing.payment.infrastructure.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
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

@SpringBootTest(properties={"app.messaging.enabled=false","spring.kafka.admin.auto-create=false","app.mock.enabled=true","app.mock.secret=synthetic-test-secret-at-least-32-characters","app.mock-delivery-delay-ms=3600000","app.reconciliation-delay-ms=3600000"})
@AutoConfigureMockMvc @Testcontainers
class CheckoutIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired CheckoutService checkout; @Autowired PaymentService payments; @Autowired PaymentRepository repo;
 @Autowired MockProvider provider; @Autowired JdbcTemplate db; @Autowired MockMvc mvc;
 @MockitoBean CommerceClient commerce; @MockitoBean IdentityClient identity; @MockitoBean JwtDecoder decoder;
 final UUID actor=UUID.randomUUID();
 @BeforeEach void config(){
  put("QR_PAYMENT","{\"status\":\"SUCCESS\",\"delaySeconds\":4,\"lifetimeSeconds\":600}");
  put("CARD_PAYMENT","{\"status\":\"SUCCESS\",\"delaySeconds\":4,\"cards\":[{\"number\":\"4242424242424242\",\"status\":\"SUCCESS\"},{\"number\":\"4000000000000002\",\"status\":\"FAILED\",\"failure\":{\"code\":\"INSUFFICIENT_FUNDS\",\"message\":\"Số dư không đủ\"}}]}");
 }
 void put(String key,String value){db.update("update payment.mock_config set value=?::jsonb where key=?",value,key);}
 Attempt start(){UUID order=UUID.randomUUID(),id=UUID.randomUUID();var o=new CommerceClient.Snapshot(order,actor,UUID.randomUUID(),UUID.randomUUID(),"Checkout fixture","PENDING_PAYMENT","PUBLISHED",100000,"VND",Instant.now().plusSeconds(600),"NONE");when(commerce.read(order)).thenReturn(o);when(commerce.claim(eq(order),eq(actor),anyString())).thenReturn(new CommerceClient.Claim(id,o));return payments.start(order,actor,UUID.randomUUID().toString());}
 CheckoutSession qr(Attempt a){return checkout.select(a.id(),actor,new MethodSelection("QR",null));}
 void due(Attempt a){db.update("update payment.mock_sessions set available_at=now()-interval '1 second' where attempt_id=?",a.id());provider.query(a.id());}
 @Test void qrIsDecodableOpaqueAndStableAcrossReloadAndReplay()throws Exception{
  var a=start();var s=qr(a);assertThat(s.attempt().paymentMethod()).isEqualTo("QR");
  assertThat(s.attempt().methodExpiresAt()).isEqualTo(a.expiresAt());
  assertThat(s.attempt().availableAt()).isBetween(s.serverTime().plusSeconds(3),s.serverTime().plusSeconds(5));
  var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(s.qrDataUri().split(",")[1]))))));
  String payload=new MultiFormatReader().decode(bitmap).getText();
  assertThat(payload).startsWith("etp:payment:").doesNotContain(actor.toString(),a.orderId().toString());
  var replay=qr(a);assertThat(replay.qrDataUri()).isEqualTo(s.qrDataUri());assertThat(replay.attempt().availableAt()).isEqualTo(s.attempt().availableAt());
  assertThat(checkout.read(a.id(),actor).attempt().status()).isEqualTo("PENDING");
  due(a);assertThat(checkout.read(a.id(),actor).qrDataUri()).isNull();
  assertThat(repo.get(a.id()).status()).isEqualTo("SUCCESS");
 }
 @Test void databaseStringFailureIsReadWithoutRestart(){put("QR_PAYMENT","\"failed\"");var a=start();qr(a);due(a);assertThat(repo.get(a.id()).status()).isEqualTo("FAILED");assertThat(repo.get(a.id()).failureCode()).isEqualTo("PAYMENT_DECLINED");}
 @Test void jsonFailureReasonIsCapturedAndConfigurationDoesNotRewriteInflight(){put("QR_PAYMENT","{\"status\":\"FAILED\",\"failure\":{\"code\":\"BANK_BUSY\",\"message\":\"Vui lòng thử lại sau\"}}");var a=start();qr(a);put("QR_PAYMENT","\"success\"");due(a);assertThat(repo.get(a.id()).status()).isEqualTo("FAILED");assertThat(repo.get(a.id()).failureMessage()).isEqualTo("Vui lòng thử lại sau");var b=start();qr(b);due(b);assertThat(repo.get(b.id()).status()).isEqualTo("SUCCESS");}
 @Test void shortLifetimeExpiresInsteadOfApprovingAfterExpiry(){put("QR_PAYMENT","{\"status\":\"SUCCESS\",\"delaySeconds\":4,\"lifetimeSeconds\":1}");var a=start();var s=qr(a);assertThat(s.attempt().availableAt()).isEqualTo(s.attempt().methodExpiresAt());due(a);assertThat(repo.get(a.id()).status()).isEqualTo("FAILED");assertThat(repo.get(a.id()).failureCode()).isEqualTo("QR_EXPIRED");}
 @Test void configuredCardsApproveOrDeclineWithoutPersistingSubmittedPan(){var a=start();checkout.select(a.id(),actor,new MethodSelection("CARD","4242 4242 4242 4242"));String stored=db.queryForObject("select row_to_json(m)::text from payment.mock_sessions m where attempt_id=?",String.class,a.id());assertThat(stored).doesNotContain("4242424242424242").contains("4242");due(a);assertThat(repo.get(a.id()).status()).isEqualTo("SUCCESS");var b=start();checkout.select(b.id(),actor,new MethodSelection("CARD","4000000000000002"));due(b);assertThat(repo.get(b.id()).status()).isEqualTo("FAILED");assertThat(repo.get(b.id()).failureCode()).isEqualTo("INSUFFICIENT_FUNDS");}
 @Test void unlistedCardCannotSucceed(){var a=start();checkout.select(a.id(),actor,new MethodSelection("CARD","1111222233334444"));due(a);assertThat(repo.get(a.id()).status()).isEqualTo("FAILED");assertThat(repo.get(a.id()).failureCode()).isEqualTo("CARD_NOT_SUPPORTED");}
 @Test void globalCardFailureOverridesAllowlistedSuccess(){db.update("update payment.mock_config set value=jsonb_set(value,'{status}','\"FAILED\"') where key='CARD_PAYMENT'");var a=start();checkout.select(a.id(),actor,new MethodSelection("CARD","4242424242424242"));due(a);assertThat(repo.get(a.id()).status()).isEqualTo("FAILED");}
 @Test void malformedCardInputAndQrCardInputAreRejectedWithoutScheduling(){var a=start();assertThatThrownBy(()->checkout.select(a.id(),actor,new MethodSelection("CARD","123"))).hasMessageContaining("16");assertThatThrownBy(()->checkout.select(a.id(),actor,new MethodSelection("QR","4242424242424242"))).hasMessageContaining("không yêu cầu");assertThat(repo.get(a.id()).paymentMethod()).isNull();}
 @Test void invalidConfigurationFailsClosedWithoutStartingSession(){var a=start();for(String value:List.of("{\"status\":\"MAYBE\"}","{\"status\":\"SUCCESS\",\"delaySeconds\":-1}","{\"status\":\"SUCCESS\",\"delaySeconds\":1.5}")){put("QR_PAYMENT",value);assertThatThrownBy(()->qr(a)).hasMessageContaining("gián đoạn");assertThat(repo.get(a.id()).paymentMethod()).isNull();}}
 @Test void expiredOrderCannotStartMethod(){var a=start();db.update("update payment.attempts set expires_at=now()-interval '1 second' where id=?",a.id());assertThatThrownBy(()->qr(a)).hasMessageContaining("không còn");}
 @Test void selectingAnotherMethodDoesNotReplacePendingSession(){var a=start();qr(a);assertThatThrownBy(()->checkout.select(a.id(),actor,new MethodSelection("CARD","4242424242424242"))).hasMessageContaining("đã chọn");assertThat(repo.get(a.id()).paymentMethod()).isEqualTo("QR");}
 @Test void concurrentSelectionAndRepeatedDeliveryHaveOneOutcome()throws Exception{var a=start();try(var pool=Executors.newFixedThreadPool(4)){var calls=new ArrayList<Future<CheckoutSession>>();for(int i=0;i<4;i++)calls.add(pool.submit(()->qr(a)));Set<Instant> dates=new HashSet<>();for(var f:calls)dates.add(f.get(10,TimeUnit.SECONDS).attempt().availableAt());assertThat(dates).hasSize(1);}due(a);provider.query(a.id());assertThat(db.queryForObject("select count(*) from payment.outbox where aggregate_id=?",Integer.class,a.orderId())).isEqualTo(1);}
 @Test void sessionAndMethodAreOwnerScopedAndScenariosAreNotCustomerControls()throws Exception{var a=start();mvc.perform(get("/api/v1/payment-sessions/"+a.id()).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isNotFound());mvc.perform(post("/api/v1/payment-sessions/"+a.id()+"/method").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString()))).contentType("application/json").content("{\"method\":\"QR\"}")).andExpect(status().isNotFound());mvc.perform(post("/api/v1/local-payments/"+a.id()+"/scenario").with(jwt().jwt(j->j.subject(actor.toString()))).contentType("application/json").content("{\"scenario\":\"SUCCESS\"}")).andExpect(status().isForbidden());}
 @Test void invalidMethodIsRejectedAndSessionResponsesAreNotCacheable()throws Exception{var a=start();mvc.perform(post("/api/v1/payment-sessions/"+a.id()+"/method").with(jwt().jwt(j->j.subject(actor.toString()))).contentType("application/json").content("{\"method\":\"OTHER\"}")).andExpect(status().isBadRequest());mvc.perform(get("/api/v1/payment-sessions/"+a.id()).with(jwt().jwt(j->j.subject(actor.toString())))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.serverTime").exists()).andExpect(jsonPath("$.attempt.paymentMethod").isEmpty());}
}
