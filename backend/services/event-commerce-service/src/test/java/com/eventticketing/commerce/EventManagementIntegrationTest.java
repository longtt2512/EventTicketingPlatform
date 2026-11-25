package com.eventticketing.commerce;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.commerce.application.*;
import com.eventticketing.commerce.application.EventModels.*;
import com.eventticketing.commerce.infrastructure.*;
import com.eventticketing.commerce.api.ApiException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties={"app.cancellation-delay-ms=3600000","app.messaging.enabled=false","spring.kafka.admin.auto-create=false","app.lifecycle-delay-ms=3600000"}) @AutoConfigureMockMvc @Testcontainers
class EventManagementIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean com.eventticketing.commerce.infrastructure.AdmissionFenceClient admissionFence;
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired EventService service;@Autowired EventRepository repo;@Autowired DraftEventService drafts;@Autowired InventoryService inventory;@Autowired EventLifecycle lifecycle;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 @MockitoBean OrganizationScopeClient scope;@MockitoBean IdentityClient identity;@MockitoBean JwtDecoder decoder;
 final UUID actor=UUID.randomUUID(),org=UUID.randomUUID();
 EventInput input(String name,Integer capacity,Instant start,UUID banner){return new EventInput(name,"Workshop","Mô tả","Hội trường","Địa chỉ","Hà Nội","Asia/Ho_Chi_Minh",start,start.plusSeconds(3600),capacity,"Điều khoản",banner,List.of());}
 EventView prepared(int quota) {
  UUID id=drafts.create(org,actor,"Event "+UUID.randomUUID()).id(),banner=UUID.randomUUID();
  db.update("insert into event_commerce.media_uploads(id,event_id,actor_id,kind,content_type,byte_size,sha256,status,object_key,expires_at) values (?,?,?,'BANNER','image/png',100,?,'AVAILABLE',?,now()+interval '1 hour')",banner,id,actor,"0".repeat(64),banner.toString());
  var e=service.update(org,id,actor,0,input("Workshop",quota,Instant.now().plusSeconds(86400),banner));
  return service.ticket(org,id,actor,e.version(),null,new TicketInput("Vé thường","",100000L,quota,Instant.now().minusSeconds(60),e.startAt().minusSeconds(1),quota));
 }
 EventView publish(EventView e){e=service.command(org,e.id(),actor,e.version(),"submit","",false);e=service.command(org,e.id(),actor,e.version(),"approve","Đạt",true);return service.command(org,e.id(),actor,e.version(),"publish","",false);}
 @Test void draftsAllowMissingScheduleButRejectPastDatesWithoutChangingRevision(){
  var draft=drafts.create(org,actor,"Date validation");
  var blank=new EventInput("Date validation",null,"","","","","Asia/Ho_Chi_Minh",null,null,null,"",null,List.of());
  var saved=service.update(org,draft.id(),actor,0,blank);
  var past=new EventInput("Date validation",null,"","","","","Asia/Ho_Chi_Minh",Instant.now().minusSeconds(7200),Instant.now().minusSeconds(3600),null,"",null,List.of());
  assertThatThrownBy(()->service.update(org,draft.id(),actor,saved.version(),past)).hasMessageContaining("bắt đầu phải ở tương lai");
  var pastEnd=new EventInput("Date validation",null,"","","","","Asia/Ho_Chi_Minh",null,Instant.now().minusSeconds(1),null,"",null,List.of());
  assertThatThrownBy(()->service.update(org,draft.id(),actor,saved.version(),pastEnd)).hasMessageContaining("kết thúc phải ở tương lai");
  assertThat(repo.get(draft.id()).version()).isEqualTo(saved.version());
  assertThat(repo.get(draft.id()).endAt()).isNull();
  var start=Instant.now().plusSeconds(86400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
  var reversed=new EventInput("Date validation",null,"","","","","Asia/Ho_Chi_Minh",start,start.minusSeconds(1),null,"",null,List.of());
  assertThatThrownBy(()->service.update(org,draft.id(),actor,saved.version(),reversed)).hasMessageContaining("kết thúc phải sau");
  var valid=new EventInput("Date validation",null,"","","","","Asia/Ho_Chi_Minh",start,start.plusSeconds(3600),null,"",null,List.of());
  assertThat(service.update(org,draft.id(),actor,saved.version(),valid).endAt()).isEqualTo(valid.endAt());
 }
 @Test void reviewRequiresCompleteDataLocksRevisionAndDoesNotAutoPublish(){
  var draft=drafts.create(org,actor,"Incomplete");assertThatThrownBy(()->service.command(org,draft.id(),actor,0,"submit","",false)).isInstanceOf(ApiException.class).hasMessageContaining("bổ sung");
  var ready=prepared(10);var pending=service.command(org,ready.id(),actor,ready.version(),"submit","",false);
  assertThatThrownBy(()->service.update(org,pending.id(),actor,pending.version(),input("Changed",10,pending.startAt(),pending.bannerMediaId()))).hasMessageContaining("PENDING_REVIEW");
  assertThatThrownBy(()->service.command(org,pending.id(),actor,0,"approve","",true)).hasMessageContaining("thay đổi");
  var approved=service.command(org,pending.id(),actor,pending.version(),"approve","",true);assertThat(approved.status()).isEqualTo("APPROVED");assertThat(approved.publishedAt()).isNull();
  var changed=service.update(org,approved.id(),actor,approved.version(),input("Revised",10,approved.startAt(),approved.bannerMediaId()));assertThat(changed.status()).isEqualTo("DRAFT");
  assertThatThrownBy(()->service.command(org,changed.id(),actor,changed.version(),"publish","",false)).hasMessageContaining("DRAFT");
 }
 @Test void publishedCoreIsLockedButPriceAndPausePreserveAllocations(){
  var e=publish(prepared(10));var ticket=e.ticketTypes().getFirst();UUID allocation=UUID.randomUUID();inventory.reserve(e.id(),allocation,Map.of(ticket.id(),2),Instant.now().plusSeconds(600));
  assertThatThrownBy(()->service.update(org,e.id(),actor,repo.get(e.id()).version(),input("Moved",10,e.startAt(),e.bannerMediaId()))).hasMessageContaining("PUBLISHED");
  service.ticket(org,e.id(),actor,ticket.version(),ticket.id(),new TicketInput(ticket.name(),ticket.description(),200000L,10,ticket.saleStart(),ticket.saleEnd(),10));
  service.pause(org,e.id(),actor,ticket.id(),1,true);assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isEqualTo(2);
  assertThatThrownBy(()->inventory.reserve(e.id(),UUID.randomUUID(),Map.of(ticket.id(),1),Instant.now().plusSeconds(600))).hasMessageContaining("chưa mở bán");
  inventory.close(e.id(),allocation,"CONSUMED");inventory.close(e.id(),allocation,"CONSUMED");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isEqualTo(2);
  assertThat(db.queryForObject("select price_snapshot from event_commerce.allocation_lines where allocation_id=?",Long.class,allocation)).isEqualTo(100000);
  assertThatThrownBy(()->service.ticket(org,e.id(),actor,2,ticket.id(),new TicketInput(ticket.name(),ticket.description(),200000L,1,ticket.saleStart(),ticket.saleEnd(),1))).isInstanceOf(ApiException.class);
 }
 @Test void concurrentReservationsCannotOversellAndMultiLineFailureRollsBack() throws Exception {
  var e=publish(prepared(10));UUID ticket=e.ticketTypes().getFirst().id();AtomicInteger success=new AtomicInteger();CountDownLatch gate=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(12)){var futures=new ArrayList<Future<?>>();for(int i=0;i<20;i++)futures.add(pool.submit(()->{try{gate.await();inventory.reserve(e.id(),UUID.randomUUID(),Map.of(ticket,1),Instant.now().plusSeconds(600));success.incrementAndGet();}catch(ApiException expected){}catch(InterruptedException ex){throw new RuntimeException(ex);}}));gate.countDown();for(var f:futures)f.get(15,TimeUnit.SECONDS);}
  assertThat(success.get()).isEqualTo(10);assertThat(repo.get(e.id()).ticketTypes().getFirst().available()).isZero();assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isEqualTo(10);
  var other=publish(prepared(5));UUID otherTicket=other.ticketTypes().getFirst().id();assertThatThrownBy(()->inventory.reserve(other.id(),UUID.randomUUID(),Map.of(otherTicket,1,UUID.randomUUID(),1),Instant.now().plusSeconds(600))).isInstanceOf(ApiException.class);
  assertThat(repo.get(other.id()).ticketTypes().getFirst().reserved()).isZero();
 }
 @Test void capacityIsAtomicAcrossTicketTypesAndCannotDropBelowQuota() throws Exception {
  var e=prepared(10);assertThatThrownBy(()->service.ticket(org,e.id(),actor,e.version(),null,new TicketInput("Extra","",0L,1,Instant.now(),e.startAt(),1))).hasMessageContaining("sức chứa");
  assertThatThrownBy(()->service.update(org,e.id(),actor,e.version(),input("Small",9,e.startAt(),e.bannerMediaId()))).hasMessageContaining("Sức chứa");
  assertThat(repo.get(e.id()).capacity()).isEqualTo(10);
 }
 @Test void cancellationReleasesOnceAndPreventsNewAllocationAndCompletionCannotBeCancelled(){
  var e=publish(prepared(10));UUID id=UUID.randomUUID(),ticket=e.ticketTypes().getFirst().id();inventory.reserve(e.id(),id,Map.of(ticket,3),Instant.now().plusSeconds(600));
  assertThatThrownBy(()->service.command(org,e.id(),actor,repo.get(e.id()).version(),"cancel","",false)).hasMessageContaining("lý do");
  var cancelled=service.command(org,e.id(),actor,repo.get(e.id()).version(),"cancel","Địa điểm đóng cửa",false);assertThat(cancelled.status()).isEqualTo("CANCELLED");assertThat(cancelled.ticketTypes().getFirst().reserved()).isZero();
  assertThatThrownBy(()->inventory.reserve(e.id(),UUID.randomUUID(),Map.of(ticket,1),Instant.now().plusSeconds(600))).hasMessageContaining("chưa mở bán");
  assertThatThrownBy(()->inventory.close(e.id(),id,"CONSUMED")).hasMessageContaining("đã được đóng");
  var ended=publish(prepared(2));db.update("update event_commerce.events set start_at=now()-interval '2 hours',end_at=now()-interval '1 hour' where id=?",ended.id());lifecycle.advance();assertThat(repo.get(ended.id()).status()).isEqualTo("COMPLETED");
  assertThatThrownBy(()->service.command(org,ended.id(),actor,repo.get(ended.id()).version(),"cancel","late",false)).hasMessageContaining("kết thúc");
 }
 @Test void expiredAllocationCannotBeConsumedAndDuplicateReserveCannotChangePayload(){
  var e=publish(prepared(4));UUID id=UUID.randomUUID(),ticket=e.ticketTypes().getFirst().id();inventory.reserve(e.id(),id,Map.of(ticket,2),Instant.now().plusSeconds(600));inventory.reserve(e.id(),id,Map.of(ticket,2),Instant.now().plusSeconds(600));
  assertThatThrownBy(()->inventory.reserve(e.id(),id,Map.of(ticket,1),Instant.now().plusSeconds(600))).hasMessageContaining("nội dung khác");db.update("update event_commerce.inventory_allocations set expires_at=now()-interval '1 second' where id=?",id);
  assertThatThrownBy(()->inventory.close(e.id(),id,"CONSUMED")).hasMessageContaining("hết hạn");inventory.close(e.id(),id,"EXPIRED");inventory.close(e.id(),id,"EXPIRED");assertThat(repo.get(e.id()).ticketTypes().getFirst().available()).isEqualTo(4);
 }
 @Test void apiRejectsMissingVersionAndNonAdminModeration() throws Exception {
  var e=prepared(2);mvc.perform(post("/api/v1/admin/events/"+e.id()+"/approve").with(jwt().jwt(j->j.subject(actor.toString()))).header("If-Match",e.version()).contentType("application/json").content("{}")) .andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/organizations/"+org+"/events/"+e.id()+"/submit").with(jwt().jwt(j->j.subject(actor.toString()))).contentType("application/json").content("{}")) .andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/organizations/"+UUID.randomUUID()+"/events/"+e.id()).with(jwt().jwt(j->j.subject(actor.toString())))).andExpect(status().isNotFound());
 }
 @Test void realPngAcceptedButSpoofedMimeChecksumAndOversizeRejected() throws Exception {
  var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);byte[] png=bytes.toByteArray();String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png));
  EventMediaService.validate(png,"image/png",hash);assertThatThrownBy(()->EventMediaService.validate(png,"image/jpeg",hash)).isInstanceOf(ApiException.class);assertThatThrownBy(()->EventMediaService.validate(png,"image/png","0".repeat(64))).isInstanceOf(ApiException.class);assertThatThrownBy(()->EventMediaService.validate(new byte[5242881],"image/png",hash)).isInstanceOf(ApiException.class);
 }
 @Test void suspendedOrganizationCannotAllocateNewTickets(){
  var e=publish(prepared(3));doThrow(new ApiException(HttpStatus.CONFLICT,"ORGANIZATION_SUSPENDED","Suspended organization")).when(scope).active(org);
  assertThatThrownBy(()->inventory.reserve(e.id(),UUID.randomUUID(),Map.of(e.ticketTypes().getFirst().id(),1),Instant.now().plusSeconds(300))).hasMessageContaining("Suspended");assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isZero();
 }
 @Test void reviewQueueSupportsStablePagination(){
  prepared(1);prepared(1);prepared(1);
  var first=service.reviewQueue("DRAFT",null,1);assertThat(first.get("hasMore")).isEqualTo(true);var second=service.reviewQueue("DRAFT",(String)first.get("nextCursor"),1);
  var a=((List<EventView>)first.get("data")).getFirst();var b=((List<EventView>)second.get("data")).getFirst();assertThat(a.id()).isNotEqualTo(b.id());
  assertThatThrownBy(()->service.reviewQueue("DRAFT","bad",1)).isInstanceOf(ApiException.class);
 }
}
