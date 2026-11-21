package com.eventticketing.finance;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.finance.application.*;
import com.eventticketing.finance.application.CommissionModels.*;
import com.eventticketing.finance.infrastructure.IdentityClient;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
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

@SpringBootTest(properties={"app.messaging.enabled=false","spring.kafka.admin.auto-create=false"})
@AutoConfigureMockMvc @Testcontainers
class CommissionIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired CommissionService service;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @MockitoBean IdentityClient identity;
    @MockitoBean JwtDecoder decoder;
    @BeforeEach void reset(){db.update("delete from finance.commission_quotes");db.update("delete from finance.audit");db.update("delete from finance.commission_configs where version>0");db.update("update finance.configuration_gate set version=0");}
    QuoteRequest request(long price,int quantity){return new QuoteRequest(UUID.randomUUID(),List.of(new Line(UUID.randomUUID(),price,quantity)));}
    @Test void percentageRoundsEachTicketAndPreservesFreeAndCap(){
        assertThat(service.quote(request(500000,2)).platformFeeVnd()).isEqualTo(50000);
        assertThat(service.quote(request(10,2)).platformFeeVnd()).isEqualTo(2);
        assertThat(service.quote(request(0,3)).platformFeeVnd()).isZero();
        assertThat(CommissionService.feePerTicket(5000,"FIXED_AMOUNT",null,10000L)).isEqualTo(5000);
        assertThat(CommissionService.feePerTicket(Long.MAX_VALUE,"PERCENTAGE",10000,null)).isEqualTo(Long.MAX_VALUE);
    }
    @Test void configChangeOnlyAffectsNewOrdersAndSameRequestReplaysOriginalSnapshot(){
        var request=request(500000,2);var old=service.quote(request);
        service.change(new Change("PERCENTAGE",700,null,null,"Future fees"),0,UUID.randomUUID());
        assertThat(service.quote(request)).isEqualTo(old);
        assertThat(service.quote(request(500000,2)).platformFeeVnd()).isEqualTo(70000);
        assertThat(db.queryForObject("select count(*) from finance.audit",Integer.class)).isEqualTo(1);
    }
    @Test void conflictingIdentityAndInvalidHybridOrRangeAreRejected(){
        var r=request(1000,1);service.quote(r);
        assertThatThrownBy(()->service.quote(new QuoteRequest(r.orderId(),List.of(new Line(r.lines().getFirst().ticketTypeId(),2000,1)))))
            .extracting(e->((com.eventticketing.finance.api.ApiException)e).getCode()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThatThrownBy(()->service.change(new Change("PERCENTAGE",500,100L,null,"Hybrid"),0,UUID.randomUUID())).hasMessageContaining("không kết hợp");
        assertThatThrownBy(()->service.change(new Change("PERCENTAGE",10001,null,null,"Invalid"),0,UUID.randomUUID())).hasMessageContaining("0–100%");
        assertThatThrownBy(()->service.change(new Change("FIXED_AMOUNT",null,-1L,null,"Invalid"),0,UUID.randomUUID())).hasMessageContaining("không âm");
    }
    @Test void futureScheduleDoesNotChangeCurrentFeeAndNoRetroactiveOrStaleUpdate(){
        service.change(new Change("FIXED_AMOUNT",null,1000L,Instant.now().plusSeconds(3600),"Scheduled fee"),0,UUID.randomUUID());
        assertThat(service.quote(request(1000,1)).platformFeeVnd()).isEqualTo(50);
        assertThatThrownBy(()->service.change(new Change("PERCENTAGE",700,null,null,"Stale"),0,UUID.randomUUID())).hasMessageContaining("đã đổi");
        assertThatThrownBy(()->service.change(new Change("PERCENTAGE",700,null,Instant.now().minusSeconds(10),"Retroactive"),1,UUID.randomUUID())).hasMessageContaining("quá khứ");
    }
    @Test void concurrentQuoteAndFeeChangeHaveOneConsistentConfiguration()throws Exception{
        var r=request(500000,2);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(3)){
            var a=pool.submit(()->{start.await();return service.quote(r);});
            var b=pool.submit(()->{start.await();return service.quote(r);});
            var c=pool.submit(()->{start.await();return service.change(new Change("PERCENTAGE",700,null,null,"Concurrent"),0,UUID.randomUUID());});
            start.countDown();var first=a.get(10,TimeUnit.SECONDS);assertThat(b.get(10,TimeUnit.SECONDS)).isEqualTo(first);c.get(10,TimeUnit.SECONDS);
            assertThat(first.platformFeeVnd()).isIn(50000L,70000L);
            assertThat(db.queryForObject("select count(*) from finance.commission_quotes where order_id=?",Integer.class,r.orderId())).isEqualTo(1);
        }
    }
    @Test void overflowFailsWithoutSavingQuote(){
        var r=request(Long.MAX_VALUE,2);assertThatThrownBy(()->service.quote(r)).hasMessageContaining("giới hạn");
        assertThat(db.queryForObject("select count(*) from finance.commission_quotes where order_id=?",Integer.class,r.orderId())).isZero();
    }
    @Test void publicAndNonAdminCannotSetFeesOrQuote()throws Exception{
        mvc.perform(get("/api/v1/admin/commission-config")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/commission-config").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(post("/internal/commission-quotes").contentType("application/json").content("{\"orderId\":\""+UUID.randomUUID()+"\",\"lines\":[{\"ticketTypeId\":\""+UUID.randomUUID()+"\",\"unitPrice\":100,\"quantity\":1}]}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SERVICE_AUTH_REQUIRED"));
    }
}
