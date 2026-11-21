package com.eventticketing.discovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.discovery.api.ApiException;
import com.eventticketing.discovery.reporting.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties={"app.messaging.enabled=false","spring.kafka.admin.auto-create=false"}) @AutoConfigureMockMvc @Testcontainers
class ReportingIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired ReportService service;@Autowired ReportingProjection projection;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
    @MockitoBean ReportAccess access;
    final Instant from=Instant.parse("2027-01-01T17:00:00Z"),to=Instant.parse("2027-01-02T17:00:00Z");
    UUID org,actor,event;
    ReportService.Filter filter(){return new ReportService.Filter(from,to,"Asia/Ho_Chi_Minh","CONFIRMED_AT",null);}
    @BeforeEach void fixture(){
        db.update("delete from discovery.report_exports");db.update("delete from discovery.report_snapshots");db.update("delete from discovery.report_entities");db.update("delete from discovery.report_inbox");
        org=UUID.randomUUID();actor=UUID.randomUUID();event=UUID.randomUUID();
        var value=json.createObjectNode().put("id",event.toString()).put("organizationId",org.toString()).put("name","Báo cáo mẫu").put("status","COMPLETED").put("capacity",100);
        value.putArray("ticketTypes").addObject().put("quota",10).put("sold",2).put("reserved",0).put("available",8);
        projection.upsert("EVENT",event,org,event,1,value);
    }
    UUID order(Instant at,long amount,boolean snapshot){
        UUID id=UUID.randomUUID();var value=json.createObjectNode().put("id",id.toString()).put("organizationId",org.toString()).put("eventId",event.toString()).put("eventName","=HYPERLINK(\"https://example.test\")").put("status","CONFIRMED").put("confirmedAt",at.toString()).put("totalVnd",amount);
        value.putArray("items").addObject().put("ticketTypeId","fixture-type").put("name","Miễn phí").put("quantity",1).put("unitPrice",amount).put("lineTotal",amount);
        if(snapshot)value.set("commissionSnapshot",json.valueToTree(Map.of("grossVnd",amount,"platformFeeVnd",0)));
        projection.upsert("ORDER",id,org,event,1,value);return id;
    }
    @Test void snapshotPaginationAndCsvAgreeEvenWhenNewOrdersArrive(){
        order(from,0,true);order(to.minusSeconds(1),0,true);
        var view=service.snapshot(org,actor,filter(),null);UUID snapshot=UUID.fromString(view.path("snapshotId").asText());
        order(from.plusSeconds(1),0,true);var same=service.snapshot(org,actor,filter(),snapshot);
        assertThat(same.path("metrics").path("ticketsSold").longValue()).isEqualTo(2);
        var page=service.sales(same,1,1);assertThat(page.get("total")).isEqualTo(2);assertThat((List<?>)page.get("data")).hasSize(1);
        String csv=service.csv(same,actor);assertThat(csv.lines().count()).isEqualTo(3);assertThat(csv).contains("' =HYPERLINK".replace("' ","'"));
        assertThat(service.snapshot(org,actor,filter(),null).path("metrics").path("ticketsSold").longValue()).isEqualTo(3);
    }
    @Test void zeroSalesAndNoAttemptsHaveNoInventedSuccessRate(){
        // BRS-T44: a missing denominator is N/A, not zero or a fabricated 100%.
        var view=service.snapshot(org,actor,filter(),null);
        assertThat(view.path("metrics").path("grossVnd").longValue()).isZero();
        assertThat(view.path("metrics").path("paymentSuccessPercent").isNull()).isTrue();
        assertThat(view.path("noSales").asBoolean()).isTrue();
    }
    @Test void halfOpenPeriodTimezoneAndQuotaDenominatorAreExplicit(){
        order(from.minusSeconds(1),0,true);order(from,0,true);order(to,0,true);
        var view=service.snapshot(org,actor,filter(),null);
        assertThat(view.path("metrics").path("ticketsSold").longValue()).isEqualTo(1);
        assertThat(view.path("metrics").path("sellThroughPercent").decimalValue()).isEqualByComparingTo("20");
        assertThat(view.path("series").get(0).path("date").asText()).isEqualTo("2027-01-02");
        assertThat(view.path("metrics").path("checkinPercent").isNull()).isTrue();
    }
    @Test void legacyFeesAndMissingEvidenceRemainPendingInsteadOfZero(){
        order(from,100000,false);var view=service.snapshot(org,actor,filter(),null);
        assertThat(view.path("metrics").path("grossVnd").longValue()).isEqualTo(100000);
        assertThat(view.path("metrics").path("netVnd").isNull()).isTrue();assertThat(view.path("metrics").path("unknownFeeOrders").longValue()).isEqualTo(1);
        assertThat(service.csv(view,actor)).contains("PENDING");
    }
    @Test void snapshotCannotBeReusedAcrossActorScopeFilterOrRevokedMembership(){
        var view=service.snapshot(org,actor,filter(),null);UUID id=UUID.fromString(view.path("snapshotId").asText());
        assertThatThrownBy(()->service.snapshot(org,UUID.randomUUID(),filter(),id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.snapshot(UUID.randomUUID(),actor,filter(),id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.snapshot(org,actor,new ReportService.Filter(from,to.plusSeconds(1),"Asia/Ho_Chi_Minh","CONFIRMED_AT",null),id)).isInstanceOf(ApiException.class);
        doThrow(new ApiException(HttpStatus.FORBIDDEN,"FINANCE_PERMISSION_REQUIRED","Revoked")).when(access).require(org,actor);
        assertThatThrownBy(()->service.snapshot(org,actor,filter(),id)).hasMessageContaining("Revoked");
    }
    @Test void staleEventDoesNotOverwriteCurrentProjectionAndPublicCannotReadFinancials()throws Exception{
        var old=json.createObjectNode().put("name","Stale");projection.upsert("EVENT",event,org,event,0,old);
        assertThat(service.snapshot(org,actor,filter(),null).path("events").get(0).path("name").asText()).isEqualTo("Báo cáo mẫu");
        mvc.perform(get("/api/v1/admin/reports/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/reports/summary").with(jwt().jwt(j->j.subject(actor.toString()))).param("from",from.toString()).param("to",to.toString()).param("timezone","Asia/Ho_Chi_Minh").param("dateBasis","CONFIRMED_AT")).andExpect(status().isForbidden());
    }
    @Test void exportNeutralizesFormulaAndKeepsEscapedVietnameseText(){
        for(String value:List.of("=1+1","+SUM(1)","-1","@test"," \t=1","\n=1"))assertThat(ReportService.csvCell(value)).startsWith("\"'");
        assertThat(ReportService.csvCell("Vé \"Đêm nhạc\", Hà Nội")).isEqualTo("\"Vé \"\"Đêm nhạc\"\", Hà Nội\"");
    }

 @Test void unmappedCashCountsGloballyWithoutInventingEventSalesOrLeakingIntoOrganizerReports()throws Exception{db.update("delete from discovery.report_entities");UUID ledger=UUID.randomUUID(),transaction=UUID.randomUUID();var payload=json.createObjectNode().put("ledgerScope","UNMAPPED").put("ledgerId",ledger.toString()).putNull("eventId").put("version",1);payload.putArray("transactions").addObject().put("id",transaction.toString()).put("amount",123).put("status","SUCCESS").put("paid_at",from.plusSeconds(1).toString());payload.putArray("returns");payload.putArray("attempts");payload.putArray("openExceptions").addObject().put("id",UUID.randomUUID().toString());String body=json.writeValueAsString(Map.of("eventId",UUID.randomUUID(),"eventVersion",1,"payload",payload));projection.accept("payment.payment.financial-updated.v1",body);projection.accept("payment.payment.financial-updated.v1",body);var global=service.snapshot(null,actor,filter(),null).path("metrics");assertThat(global.path("cashCollectedVnd").asLong()).isEqualTo(123);assertThat(global.path("grossVnd").asLong()).isZero();assertThat(global.path("openExceptions").asInt()).isEqualTo(1);assertThat(service.snapshot(org,actor,filter(),null).path("metrics").path("cashCollectedVnd").asLong()).isZero();}
}
