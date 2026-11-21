package com.eventticketing.payment;

import static org.assertj.core.api.Assertions.*;
import com.eventticketing.payment.application.*;
import com.eventticketing.payment.infrastructure.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Real PostgreSQL locks and HTTP boundary; the organization response is controlled. */
@SpringBootTest(properties={"app.messaging.enabled=false","spring.kafka.admin.auto-create=false","app.reconciliation-delay-ms=3600000"})
@Testcontainers
class PayoutAuthorizationIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
    static final AtomicInteger calls=new AtomicInteger();
    static volatile int responseStatus=200;
    static volatile CountDownLatch entered,release;
    static final HttpServer server=server();
    static HttpServer server(){try{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/internal/organizations/",exchange->{
            calls.incrementAndGet();exchange.getRequestBody().readAllBytes();
            if(entered!=null){entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}}
            byte[] body="{\"proof\":\"durable-organization-authorization\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(responseStatus,body.length);
            try(var out=exchange.getResponseBody()){out.write(body);}
        });server.start();return server;
    }catch(Exception ex){throw new IllegalStateException(ex);}}
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);r.add("app.organization-base-url",()->"http://127.0.0.1:"+server.getAddress().getPort());}
    @Autowired PayoutAuthorization authorization;@Autowired FinancialLedger ledger;@Autowired JdbcTemplate db;@Autowired PlatformTransactionManager transactions;
    @MockitoBean CommerceClient commerce;@MockitoBean IdentityClient identity;@MockitoBean JwtDecoder decoder;
    @BeforeEach void reset(){calls.set(0);responseStatus=200;entered=null;release=null;db.update("delete from payment.exceptions");}
    @AfterAll static void stop(){server.stop(0);}
    PayoutAuthorization.Request request(UUID event){return new PayoutAuthorization.Request(UUID.randomUUID(),event,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,(String)ledger.snapshot(event).get("fingerprint"),"evidence-"+UUID.randomUUID());}
    void exception(UUID event){UUID transaction=UUID.randomUUID();db.update("insert into payment.transactions(id,event_id,provider,merchant,reference,amount,currency,status) values (?,?,'local-mock','test',?,100,'VND','SUCCESS')",transaction,event,"MOCK-"+transaction);db.update("insert into payment.exceptions(id,transaction_id,event_id,type,status,received_amount,returned_amount,currency) values (?,?,?,'UNKNOWN_TRANSACTION','OPEN',100,0,'VND')",UUID.randomUUID(),transaction,event);}

    @Test void durableProofReplaysWithoutSecondAuthorizationAndConflictingReuseFails(){
        var request=request(UUID.randomUUID());var proof=authorization.authorize(request);exception(request.eventId());
        assertThat(authorization.authorize(request)).isEqualTo(proof);assertThat(calls.get()).isEqualTo(1);
        var changed=new PayoutAuthorization.Request(request.settlementId(),request.eventId(),request.organizationId(),request.userId(),request.payoutId(),2,request.paymentFingerprint(),request.evidenceFingerprint());
        assertThatThrownBy(()->authorization.authorize(changed)).hasMessageContaining("nội dung khác");
        assertThat(db.queryForObject("select count(*) from payment.payout_authorizations where settlement_id=?",Integer.class,request.settlementId())).isEqualTo(1);
    }
    @Test void changedFinancialSnapshotAndOpenExceptionCannotReachOrganization(){
        UUID event=UUID.randomUUID();var stale=request(event);exception(event);
        assertThatThrownBy(()->authorization.authorize(stale)).hasMessageContaining("nghĩa vụ đã đổi");
        assertThatThrownBy(()->authorization.authorize(request(event))).hasMessageContaining("ngoại lệ tài chính");assertThat(calls.get()).isZero();
    }
    @Test void unknownHttpOutcomeRetainsNoFalseProofAndSameRequestCanRecover(){
        var request=request(UUID.randomUUID());responseStatus=503;
        assertThatThrownBy(()->authorization.authorize(request)).hasMessageContaining("Chưa biết kết quả");
        assertThat(db.queryForObject("select count(*) from payment.payout_authorizations where settlement_id=?",Integer.class,request.settlementId())).isZero();
        responseStatus=200;assertThat(authorization.authorize(request).path("proof").asText()).contains("durable");assertThat(calls.get()).isEqualTo(2);
    }
    @Test void paymentMutationWaitsForAuthorizationCommitAcrossHttpBoundary()throws Exception{
        var request=request(UUID.randomUUID());entered=new CountDownLatch(1);release=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var proof=pool.submit(()->authorization.authorize(request));assertThat(entered.await(2,TimeUnit.SECONDS)).isTrue();
            var attempted=new CountDownLatch(1);
            var mutation=pool.submit(()->new TransactionTemplate(transactions).execute(status->{attempted.countDown();ledger.lock(request.eventId());exception(request.eventId());return true;}));
            assertThat(attempted.await(1,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(()->mutation.get(150,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();assertThat(proof.get(3,TimeUnit.SECONDS).path("proof").asText()).contains("durable");assertThat(mutation.get(3,TimeUnit.SECONDS)).isTrue();
            assertThat(authorization.authorize(request).path("proof").asText()).contains("durable");
        }finally{release.countDown();}
    }
}
