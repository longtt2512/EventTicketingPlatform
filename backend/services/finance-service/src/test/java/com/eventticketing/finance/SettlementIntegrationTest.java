package com.eventticketing.finance;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.finance.api.ApiException;
import com.eventticketing.finance.application.*;
import com.eventticketing.finance.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties={"app.messaging.enabled=false","spring.kafka.admin.auto-create=false","app.settlement-recovery-delay-ms=3600000"})
@AutoConfigureMockMvc @Testcontainers
class SettlementIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired SettlementService service;@Autowired SettlementCompletion completion;@Autowired SettlementRepository repo;
    @Autowired FinanceProjection projection;@Autowired JdbcTemplate db;@Autowired ObjectMapper json;@Autowired MockMvc mvc;
    @MockitoBean FinanceSources sources;@MockitoBean IdentityClient identity;@MockitoBean JwtDecoder decoder;
    UUID org,event,actor,order,transaction;ObjectNode organization,commerce,payment;
    @BeforeEach void fixture(){
        db.update("delete from finance.settlement_requests");db.update("delete from finance.settlement_items");db.update("delete from finance.settlements");db.update("delete from finance.audit");
        org=UUID.randomUUID();event=UUID.randomUUID();actor=UUID.randomUUID();order=UUID.randomUUID();transaction=UUID.randomUUID();
        organization=json.valueToTree(Map.of("id",org,"status","ACTIVE","payout",Map.of("id",UUID.randomUUID(),"destination_version",1,"masked_account","•••• 1234","bank_name","Ngân hàng mẫu")));
        commerce=json.createObjectNode();commerce.set("event",json.valueToTree(Map.of("id",event,"organizationId",org,"name","Sự kiện kiểm thử","status","COMPLETED","version",9)));
        commerce.putArray("orders").addObject().put("id",order.toString()).put("organizationId",org.toString()).put("eventId",event.toString()).put("status","CONFIRMED").put("totalVnd",1000000)
            .put("appliedPaymentId",transaction.toString()).set("commissionSnapshot",json.valueToTree(Map.of("configId",UUID.randomUUID(),"grossVnd",1000000,"platformFeeVnd",50000)));
        payment=json.createObjectNode().put("eventId",event.toString()).put("fingerprint","payment-fingerprint-1");
        payment.putArray("transactions").addObject().put("id",transaction.toString()).put("order_id",order.toString()).put("amount",1000000).put("currency","VND").put("status","SUCCESS").put("result","APPLIED").put("fee_vnd",10000).put("reconciled_at",Instant.now().toString()).put("provenance","LOCAL_PROVIDER_LEDGER");
        payment.putArray("returns");payment.putArray("attempts");payment.putArray("openExceptions");
        when(sources.organization(eq(org),any(),anyBoolean())).thenAnswer(x->organization.deepCopy());
        when(sources.commerce(event)).thenAnswer(x->commerce.deepCopy());when(sources.payment(event)).thenAnswer(x->payment.deepCopy());
        when(sources.authorize(any())).thenReturn(json.valueToTree(Map.of("authorized_at",Instant.now(),"payout_id",organization.path("payout").path("id").asText())));
    }
    UUID request(){return UUID.fromString(service.request(org,actor,UUID.randomUUID().toString(),new SettlementService.Request(event)).path("id").asText());}
    long version(UUID id){return repo.get(id).path("version").longValue();}
    void process(UUID id){service.process(id,actor,version(id),"Kiểm tra đầy đủ điều kiện");}
    SettlementService.Evidence evidence(long amount){return new SettlementService.Evidence(amount==0?"NO_TRANSFER":"PAID",amount,amount==0?null:"MOCK-SETTLEMENT-"+UUID.randomUUID(),Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),"Bằng chứng mô phỏng hợp lệ");}
    @Test void reconciledAmountsAndIdempotentClaimKeepOneEntitlement(){
        var preview=service.preview(org,event,actor);assertThat(preview.path("netVnd").longValue()).isEqualTo(940000);
        String key=UUID.randomUUID().toString();var input=new SettlementService.Request(event);var a=service.request(org,actor,key,input);var b=service.request(org,actor,key,input);
        assertThat(a.path("id")).isEqualTo(b.path("id"));assertThat(db.queryForObject("select count(*) from finance.settlement_items",Integer.class)).isEqualTo(1);
        assertThatThrownBy(this::request).extracting(e->((ApiException)e).getCode()).isEqualTo("ALREADY_CLAIMED");
    }
    @Test void concurrentRequestsCannotClaimTheEventTwice()throws Exception{
        var start=new CountDownLatch(1);var successes=new java.util.concurrent.atomic.AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(6)){var futures=new ArrayList<Future<?>>();for(int i=0;i<6;i++)futures.add(pool.submit(()->{start.await();try{request();successes.incrementAndGet();}catch(ApiException ex){assertThat(ex.getCode()).isEqualTo("ALREADY_CLAIMED");}return null;}));start.countDown();for(var f:futures)f.get(10,TimeUnit.SECONDS);}
        assertThat(successes.get()).isEqualTo(1);assertThat(db.queryForObject("select count(*) from finance.settlement_items",Integer.class)).isEqualTo(1);
    }
    @Test void missingFeeLegacySnapshotAndExceptionNeverBecomePayable(){
        ((ObjectNode)payment.path("transactions").get(0)).putNull("fee_vnd");assertThat(service.preview(org,event,actor).path("netVnd").isNull()).isTrue();assertThatThrownBy(this::request).hasMessageContaining("GATEWAY_FEE_UNKNOWN");
        ((ObjectNode)payment.path("transactions").get(0)).put("fee_vnd",10000);
        ((ObjectNode)commerce.path("orders").get(0)).remove("commissionSnapshot");assertThatThrownBy(this::request).hasMessageContaining("COMMISSION_SNAPSHOT_MISSING");
        assertThat(db.queryForObject("select count(*) from finance.settlements",Integer.class)).isZero();
    }
    @Test void incompleteCancelledAndPendingPaymentEventsStayBlocked(){
        for(String state:List.of("PUBLISHED","ONGOING","CANCELLED")){((ObjectNode)commerce.path("event")).put("status",state);assertThatThrownBy(this::request).hasMessageContaining("EVENT_NOT_COMPLETED");}
        ((ObjectNode)commerce.path("event")).put("status","COMPLETED");payment.withArray("attempts").addObject().put("status","PENDING");assertThatThrownBy(this::request).hasMessageContaining("PAYMENT_UNKNOWN");
    }
    @Test void sourceChangesAfterReviewBlockEvidenceAndDestinationNeedsExplicitRefresh(){
        UUID id=request();((ObjectNode)organization.path("payout")).put("id",UUID.randomUUID().toString()).put("destination_version",2);
        assertThatThrownBy(()->process(id)).hasMessageContaining("Tài khoản đã đổi");
        service.destination(id,actor,version(id),"Xác minh tài khoản mới trước khi trả");process(id);
        payment.withArray("openExceptions").addObject().put("id",UUID.randomUUID().toString());
        assertThatThrownBy(()->service.prepare(id,actor,version(id),evidence(940000))).hasMessageContaining("PAYMENT_EXCEPTION_OPEN");
        assertThat(repo.get(id).path("decision").isNull()).isTrue();
    }
    @Test void staleActionCannotOverwriteReview(){UUID id=request();long before=version(id);process(id);assertThatThrownBy(()->service.fail(id,actor,before,"Chắc chắn chưa trả tiền",false)).hasMessageContaining("đã thay đổi");}
    @Test void successfulEvidenceSettlesExactlyOnceAndKeepsAudit(){
        UUID id=request();process(id);var input=evidence(940000);service.prepare(id,actor,version(id),input);completion.complete(id);completion.complete(id);
        assertThat(repo.get(id).path("status").asText()).isEqualTo("SETTLED");
        assertThat(db.queryForObject("select count(*) from finance.audit where resource_id=? and action='SETTLEMENT_COMPLETED'",Integer.class,id)).isEqualTo(1);
        assertThatThrownBy(()->service.fail(id,actor,version(id),"Đã trả không được trả lại",false)).hasMessageContaining("quyết định");
    }
    @Test void paidReferenceCannotBeReusedForAnotherEventSettlement(){
        // BRS-T38: retain the first payout, reject duplicate evidence on a separate claim.
        UUID first=request();process(first);var paid=evidence(940000);
        service.prepare(first,actor,version(first),paid);completion.complete(first);
        event=UUID.randomUUID();order=UUID.randomUUID();transaction=UUID.randomUUID();
        ((ObjectNode)commerce.path("event")).put("id",event.toString());
        ((ObjectNode)commerce.path("orders").get(0)).put("id",order.toString()).put("eventId",event.toString()).put("appliedPaymentId",transaction.toString());
        payment.put("eventId",event.toString()).put("fingerprint","payment-fingerprint-2");
        ((ObjectNode)payment.path("transactions").get(0)).put("id",transaction.toString()).put("order_id",order.toString());
        when(sources.commerce(event)).thenAnswer(x->commerce.deepCopy());when(sources.payment(event)).thenAnswer(x->payment.deepCopy());
        UUID second=request();process(second);
        var duplicate=new SettlementService.Evidence("PAID",940000,paid.reference(),Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),"Same reference must not pay another event");
        assertThatThrownBy(()->service.prepare(second,actor,version(second),duplicate)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class).hasMessageContaining("transfer_reference");
        assertThat(repo.get(second).path("decision").isNull()).isTrue();
        assertThat(repo.get(second).path("status").asText()).isEqualTo("PROCESSING");
        assertThat(repo.get(first).path("status").asText()).isEqualTo("SETTLED");
        service.prepare(second,actor,version(second),evidence(940000));completion.complete(second);
        assertThat(db.queryForObject("select count(distinct transfer_reference) from finance.settlements where status='SETTLED'",Integer.class)).isEqualTo(2);
    }
    @Test void lostAuthorizationResponseKeepsEvidenceAndRecoversSameRequest(){
        UUID id=request();process(id);var input=evidence(940000);long before=version(id);service.prepare(id,actor,before,input);
        when(sources.authorize(any())).thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"DEPENDENCY_UNAVAILABLE","Lost response")).thenReturn(json.createObjectNode().put("authorized_at",Instant.now().toString()));
        assertThatThrownBy(()->completion.complete(id)).hasMessageContaining("Lost response");assertThat(repo.get(id).path("transferStatus").asText()).isEqualTo("UNKNOWN");
        assertThatThrownBy(()->service.prepare(id,actor,version(id),evidence(940000))).hasMessageContaining("bằng chứng khác");
        service.prepare(id,actor,before,input);completion.complete(id);assertThat(repo.get(id).path("status").asText()).isEqualTo("SETTLED");
    }
    @Test void unknownCannotRetryUntilDefiniteFailureAndClaimIsRetained(){
        UUID id=request();process(id);service.fail(id,actor,version(id),"Chưa rõ kết quả chuyển tiền",true);assertThatThrownBy(()->process(id)).hasMessageContaining("chắc chắn");
        service.fail(id,actor,version(id),"Bằng chứng xác nhận tiền chưa chuyển",false);process(id);
        assertThat(db.queryForObject("select count(*) from finance.settlement_items where settlement_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void zeroAmountClosesWithoutFabricatedTransferReference(){
        var orderNode=(ObjectNode)commerce.path("orders").get(0);orderNode.put("totalVnd",0);orderNode.remove("appliedPaymentId");orderNode.set("commissionSnapshot",json.valueToTree(Map.of("grossVnd",0,"platformFeeVnd",0)));payment.putArray("transactions");
        UUID id=request();process(id);service.prepare(id,actor,version(id),evidence(0));completion.complete(id);
        assertThat(repo.get(id).path("transferStatus").asText()).isEqualTo("NO_TRANSFER");assertThat(repo.get(id).path("transferReference").isNull()).isTrue();
    }
    @Test void appliedReturnReversesFeeOnlyWhenFullAndExtraReturnDoesNotReverseSaleFee(){
        payment.withArray("returns").addObject().put("transaction_id",UUID.randomUUID().toString()).put("amount",1000000);
        assertThat(service.preview(org,event,actor).path("platformFeeReversalVnd").longValue()).isZero();
        payment.withArray("returns").addObject().put("transaction_id",transaction.toString()).put("amount",300000);
        assertThat(service.preview(org,event,actor).path("netVnd").isNull()).isTrue();
        payment.withArray("returns").addObject().put("transaction_id",transaction.toString()).put("amount",700000);
        var preview=service.preview(org,event,actor);assertThat(preview.path("platformFeeReversalVnd").longValue()).isEqualTo(50000);assertThat(preview.path("netVnd").longValue()).isEqualTo(-10000);assertThat(preview.path("eligible").asBoolean()).isFalse();
    }
    @Test void unprivilegedCallerCannotReachAdminAndForeignOrgIsDenied()throws Exception{
        UUID id=request();mvc.perform(get("/api/v1/admin/settlements").with(jwt())).andExpect(status().isForbidden());
        assertThatThrownBy(()->service.read(UUID.randomUUID(),id,actor)).isInstanceOf(ApiException.class);
    }
}
