package com.eventticketing.commerce;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.commerce.application.*;
import com.eventticketing.commerce.application.EventModels.*;
import com.eventticketing.commerce.application.OrderModels.*;
import com.eventticketing.commerce.infrastructure.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
@SpringBootTest(properties={"app.cancellation-delay-ms=3600000","app.messaging.enabled=false","spring.kafka.admin.auto-create=false","app.lifecycle-delay-ms=3600000","app.order-expiry-delay-ms=3600000"}) @AutoConfigureMockMvc @Testcontainers
class OrderIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean com.eventticketing.commerce.infrastructure.AdmissionFenceClient admissionFence;
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired OrderService orders;@Autowired OrderRepository orderRepo;@Autowired EventService events;@Autowired EventRepository repo;@Autowired DraftEventService drafts;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 @MockitoBean CommissionClient commission;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @MockitoBean OrganizationScopeClient scope;@MockitoBean IdentityClient identity;@MockitoBean JwtDecoder decoder;
 final UUID actor=UUID.randomUUID(),org=UUID.randomUUID();
 @BeforeEach void setup(){
  when(commission.quote(any(),anyList())).thenAnswer(call->{
   java.util.List<java.util.Map<String,Object>> lines=call.getArgument(1);long gross=0,fees=0;
   for(var line:lines){long price=((Number)line.get("unitPrice")).longValue();int qty=((Number)line.get("quantity")).intValue();gross=Math.addExact(gross,Math.multiplyExact(price,qty));fees=Math.addExact(fees,Math.multiplyExact(Math.round(price*0.05),qty));}
   return json.valueToTree(Map.of("configId",UUID.randomUUID(),"mode","PERCENTAGE","rateBps",500,"capturedAt",Instant.now(),"grossVnd",gross,"platformFeeVnd",fees));
  });when(identity.securityState(any())).thenAnswer(a->new IdentityClient.SecurityState(a.getArgument(0),"ACTIVE",true,Set.of("CUSTOMER")));}
 EventView event(int quota,long price){return event(quota,price,0);}
 EventView event(int quota,long price,int freeQuota){UUID id=drafts.create(org,actor,"Phase 3 test").id(),banner=UUID.randomUUID();Instant start=Instant.now().plusSeconds(86400);
 db.update("insert into event_commerce.media_uploads(id,event_id,actor_id,kind,content_type,byte_size,sha256,status,object_key,expires_at) values (?,?,?,'BANNER','image/png',100,?,'AVAILABLE',?,now()+interval '1 hour')",banner,id,actor,"0".repeat(64),banner.toString());
 var e=events.update(org,id,actor,0,new EventInput("Phase 3 test","Workshop","Description","Venue","Address","Hà Nội","Asia/Ho_Chi_Minh",start,start.plusSeconds(3600),quota+freeQuota,"Terms",banner,List.of()));
 e=events.ticket(org,id,actor,e.version(),null,new TicketInput("Standard","",price,quota,Instant.now().minusSeconds(30),start.minusSeconds(1),quota));
 if(freeQuota>0)e=events.ticket(org,id,actor,e.version(),null,new TicketInput("Free","",0L,freeQuota,Instant.now().minusSeconds(30),start.minusSeconds(1),freeQuota));
 e=events.command(org,id,actor,e.version(),"submit","",false);e=events.command(org,id,actor,e.version(),"approve","",true);return events.command(org,id,actor,e.version(),"publish","",false);}
 Reserve input(EventView e,int count){return new Reserve(e.id(),List.of(new Line(e.ticketTypes().getFirst().id(),count)),orders.quote(e.id()).selectionVersion(),true);}
 Order reserve(EventView e){return orders.reserve(actor,UUID.randomUUID().toString(),input(e,1));}
 Evidence evidence(Order o,UUID attempt,long amount,String status){return new Evidence(UUID.randomUUID(),attempt,o.id(),amount,"VND",status);}
 void expired(Order o){db.update("update event_commerce.orders set expires_at=now()-interval '1 second' where id=?",o.id());db.update("update event_commerce.inventory_allocations set expires_at=now()-interval '1 second' where id=?",o.id());}
 @Test void reserveSnapshotsAndReplayWithoutExtendingExpiry(){var e=event(4,100000);var input=input(e,2);String key=UUID.randomUUID().toString();var o=orders.reserve(actor,key,input);var replay=orders.reserve(actor,key,input);assertThat(replay.id()).isEqualTo(o.id());assertThat(replay.expiresAt()).isEqualTo(o.expiresAt());assertThat(o.totalVnd()).isEqualTo(200000);assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isEqualTo(2);assertThatThrownBy(()->orders.reserve(actor,key,input(e,1))).hasMessageContaining("nội dung khác");}
 @Test void concurrentRequestsNeverOversell(){var e=event(10,1000);var input=input(e,1);var success=new AtomicInteger();var start=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(10)){var tasks=new ArrayList<Future<?>>();for(int i=0;i<20;i++)tasks.add(pool.submit(()->{try{start.await();orders.reserve(actor,UUID.randomUUID().toString(),input);success.incrementAndGet();}catch(com.eventticketing.commerce.api.ApiException expected){}catch(InterruptedException ex){Thread.currentThread().interrupt();}}));start.countDown();tasks.forEach(f->{try{f.get(20,TimeUnit.SECONDS);}catch(Exception ex){throw new RuntimeException(ex);}});}assertThat(success.get()).isEqualTo(10);assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isEqualTo(10);}

 @Test void hundredSimultaneousBuyersCompeteForExactlyOneFinalTicket() throws Exception {
  // BRS-T09: all contenders reach the barrier before the last seat is offered.
  var e = event(1, 1000);
  var selection = input(e, 1);
  var ready = new CountDownLatch(100);
  var start = new CountDownLatch(1);
  var outcomes = new ArrayList<Future<String>>();
  try (var pool = Executors.newFixedThreadPool(100)) {
   for (int i = 0; i < 100; i++) outcomes.add(pool.submit(() -> {
    ready.countDown();
    if (!start.await(15, TimeUnit.SECONDS)) throw new AssertionError("BRS-T09 start barrier timed out");
    try {
     orders.reserve(UUID.randomUUID(), UUID.randomUUID().toString(), selection);
     return "RESERVED";
    } catch (com.eventticketing.commerce.api.ApiException rejected) {
     return rejected.getCode();
    }
   }));
   assertThat(ready.await(15, TimeUnit.SECONDS)).as("all 100 contenders are ready").isTrue();
   start.countDown();
   var results = new ArrayList<String>();
   for (var outcome : outcomes) results.add(outcome.get(30, TimeUnit.SECONDS));
   assertThat(results).filteredOn("RESERVED"::equals).hasSize(1);
   assertThat(results).filteredOn(x -> !x.equals("RESERVED")).containsOnly("INSUFFICIENT_INVENTORY").hasSize(99);
  } finally {
   start.countDown();
  }
  var stock = repo.get(e.id()).ticketTypes().getFirst();
  assertThat(stock.reserved()).isEqualTo(1);
  assertThat(stock.sold()).isZero();
  assertThat(db.queryForObject("select count(*) from event_commerce.orders where event_id=?", Integer.class, e.id())).isEqualTo(1);
 }
 @Test void invalidMixedSelectionDoesNotLeaveHoldOrOrder(){var e=event(3,1000);var input=new Reserve(e.id(),List.of(new Line(e.ticketTypes().getFirst().id(),1),new Line(UUID.randomUUID(),1)),orders.quote(e.id()).selectionVersion(),true);assertThatThrownBy(()->orders.reserve(actor,UUID.randomUUID().toString(),input)).isInstanceOf(RuntimeException.class);assertThat(db.queryForObject("select count(*) from event_commerce.orders where event_id=?",Integer.class,e.id())).isZero();assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isZero();}
 @Test void duplicateLinesRespectCombinedLimit(){var e=event(3,1000);var t=e.ticketTypes().getFirst().id();var input=new Reserve(e.id(),List.of(new Line(t,2),new Line(t,2)),orders.quote(e.id()).selectionVersion(),true);assertThatThrownBy(()->orders.reserve(actor,UUID.randomUUID().toString(),input)).hasMessageContaining("giới hạn");assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isZero();}
 @Test void stalePriceRequiresReconfirmation(){var e=event(3,1000);var input=input(e,1);db.update("update event_commerce.ticket_types set price=2000,version=version+1 where id=?",e.ticketTypes().getFirst().id());assertThatThrownBy(()->orders.reserve(actor,UUID.randomUUID().toString(),input)).hasMessageContaining("thay đổi");}
 @Test void unverifiedCustomerCannotBook(){var e=event(3,1000);when(identity.securityState(actor)).thenReturn(new IdentityClient.SecurityState(actor,"ACTIVE",false,Set.of("CUSTOMER")));assertThatThrownBy(()->reserve(e)).hasMessageContaining("xác thực email");}
 @Test void freeConfirmAndReplayConsumeExactlyOnce(){var e=event(3,0);var o=reserve(e);String key=UUID.randomUUID().toString();assertThat(orders.command(o.id(),actor,key,"confirm-free").status()).isEqualTo("CONFIRMED");assertThat(orders.command(o.id(),actor,key,"confirm-free").status()).isEqualTo("CONFIRMED");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isEqualTo(1);assertThat(db.queryForObject("select count(*) from event_commerce.event_outbox where aggregate_id=? and topic='event.order.confirmed.v1'",Integer.class,o.id())).isEqualTo(1);}
 @Test void foreignOwnerCannotReadOrCancel(){var o=reserve(event(3,1000));assertThatThrownBy(()->orders.read(o.id(),UUID.randomUUID(),false)).hasMessageContaining("quyền");assertThatThrownBy(()->orders.command(o.id(),UUID.randomUUID(),UUID.randomUUID().toString(),"cancel")).hasMessageContaining("quyền");}
 @Test void expiryReleasesOnceAndRejectsFreeConfirmation(){var e=event(3,0);var o=reserve(e);expired(o);orders.expire(o.id());orders.expire(o.id());assertThat(orderRepo.get(o.id()).status()).isEqualTo("EXPIRED");assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isZero();assertThatThrownBy(()->orders.command(o.id(),actor,UUID.randomUUID().toString(),"confirm-free")).hasMessageContaining("điều kiện");}
 @Test void pendingPaymentBlocksCancelAndNewAttemptButSameKeyRecovers(){var o=reserve(event(3,1000));String key=UUID.randomUUID().toString();var claim=orders.claim(o.id(),new Claim(actor,key));assertThat(orders.claim(o.id(),new Claim(actor,key)).attemptId()).isEqualTo(claim.attemptId());assertThatThrownBy(()->orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()))).hasMessageContaining("thanh toán mới");assertThatThrownBy(()->orders.command(o.id(),actor,UUID.randomUUID().toString(),"cancel")).hasMessageContaining("thanh toán đang chờ");}
 @Test void failurePermitsSafeRetryAndOriginalExpiryRemains(){var o=reserve(event(3,1000));var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));orders.apply(UUID.randomUUID(),evidence(o,c.attemptId(),1000,"FAILED"));var retry=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));assertThat(retry.attemptId()).isNotEqualTo(c.attemptId());assertThat(retry.order().expiresAt()).isEqualTo(o.expiresAt());}
 @Test void paidSuccessDuplicateAndFailureAfterSuccessAreSafe(){var e=event(3,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));var p=evidence(o,c.attemptId(),1000,"SUCCESS");orders.apply(UUID.randomUUID(),p);orders.apply(UUID.randomUUID(),p);orders.apply(UUID.randomUUID(),evidence(o,c.attemptId(),1000,"FAILED"));assertThat(orderRepo.get(o.id()).status()).isEqualTo("CONFIRMED");assertThat(orderRepo.get(o.id()).paymentStatus()).isEqualTo("SUCCESS");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isEqualTo(1);}
 @Test void lateCollectionNeverReacquiresReleasedStock(){var e=event(1,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));expired(o);orders.expire(o.id());var other=orders.reserve(actor,UUID.randomUUID().toString(),input(e,1));orders.apply(UUID.randomUUID(),evidence(o,c.attemptId(),1000,"SUCCESS"));assertThat(orderRepo.get(o.id()).status()).isEqualTo("EXPIRED");assertThat(orderRepo.get(o.id()).paymentStatus()).isEqualTo("EXCEPTION");assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isEqualTo(1);assertThat(orderRepo.get(other.id()).status()).isEqualTo("PENDING_PAYMENT");}
 @Test void wrongAmountAndCurrencyNeverConfirm(){var e=event(3,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));orders.apply(UUID.randomUUID(),new Evidence(UUID.randomUUID(),c.attemptId(),o.id(),1000,"USD","SUCCESS"));assertThat(orderRepo.get(o.id()).status()).isEqualTo("PENDING_PAYMENT");assertThat(orderRepo.get(o.id()).paymentStatus()).isEqualTo("EXCEPTION");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isZero();}
 @Test void cancelledEventAtomicallyClosesPendingOrdersAndLateSuccessIsException(){var e=event(3,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));var current=repo.get(e.id());events.command(org,e.id(),actor,current.version(),"cancel","Local test",false);assertThat(orderRepo.get(o.id()).status()).isEqualTo("CANCELLED");orders.apply(UUID.randomUUID(),evidence(o,c.attemptId(),1000,"SUCCESS"));assertThat(orderRepo.get(o.id()).status()).isEqualTo("CANCELLED");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isZero();}
 @Test void secondDistinctCollectionDoesNotAllocateAgain(){var e=event(3,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));orders.apply(UUID.randomUUID(),evidence(o,c.attemptId(),1000,"SUCCESS"));orders.apply(UUID.randomUUID(),evidence(o,c.attemptId(),1000,"SUCCESS"));assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isEqualTo(1);assertThat(orderRepo.get(o.id()).paymentStatus()).isEqualTo("EXCEPTION");}
 @Test void historyHasStablePaginationAndNoForeignOrders(){var e=event(3,1000);reserve(e);reserve(e);var first=orders.list(actor,null,null,1);assertThat(first.hasMore()).isTrue();var next=orders.list(actor,null,first.nextCursor(),1);assertThat(next.data().getFirst().id()).isNotEqualTo(first.data().getFirst().id());assertThat(orders.list(UUID.randomUUID(),null,null,20).data()).isEmpty();}
 @Test void publicCannotInjectPaidConfirmationAndInvalidReserveIs400()throws Exception{var o=reserve(event(3,1000));mvc.perform(post("/api/v1/orders/"+o.id()+"/confirm-free").with(jwt().jwt(j->j.subject(actor.toString()))).header("Idempotency-Key",UUID.randomUUID().toString())).andExpect(status().isConflict());mvc.perform(post("/api/v1/orders/reservations").with(jwt().jwt(j->j.subject(actor.toString()))).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{}" )).andExpect(status().isBadRequest());mvc.perform(get("/internal/orders/"+o.id()).header("X-Service-Token","wrong")).andExpect(status().isForbidden());}
 @Test void expiryAndConfirmationRaceConservesInventory(){var e=event(1,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));expired(o);var evidence=evidence(o,c.attemptId(),1000,"SUCCESS");try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->orders.expire(o.id()));var b=pool.submit(()->orders.apply(UUID.randomUUID(),evidence));try{a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}catch(Exception ex){throw new RuntimeException(ex);}}assertThat(orderRepo.get(o.id()).status()).isEqualTo("EXPIRED");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isZero();assertThat(repo.get(e.id()).ticketTypes().getFirst().reserved()).isZero();}
 @Test void returningExtraCollectionPreservesOriginalSuccessfulOrder(){var e=event(3,1000);var o=reserve(e);var c=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));var first=evidence(o,c.attemptId(),1000,"SUCCESS");var extra=evidence(o,c.attemptId(),1000,"SUCCESS");orders.apply(UUID.randomUUID(),first);orders.apply(UUID.randomUUID(),extra);orders.apply(UUID.randomUUID(),new Evidence(extra.paymentId(),extra.attemptId(),o.id(),1000,"VND","REFUNDED"));assertThat(orderRepo.get(o.id()).paymentStatus()).isEqualTo("SUCCESS");assertThat(orderRepo.get(o.id()).status()).isEqualTo("CONFIRMED");assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isEqualTo(1);}

 @Autowired EventCancellation cancellation;
 @Test void cancellationFenceFailurePersistsRequestBlocksAllocationAndRecovers(){var e=event(3,0);var o=reserve(e);doThrow(new IllegalStateException("Admission offline")).when(admissionFence).freeze(e.id());var current=repo.get(e.id());assertThatThrownBy(()->events.command(org,e.id(),actor,current.version(),"cancel","Cancelled test",false)).isInstanceOf(com.eventticketing.commerce.api.ApiException.class).extracting(x->((com.eventticketing.commerce.api.ApiException)x).getCode()).isEqualTo("CANCELLATION_PENDING");assertThat(repo.get(e.id()).cancellationPending()).isTrue();assertThat(repo.get(e.id()).status()).isEqualTo("PUBLISHED");assertThat(orderRepo.get(o.id()).status()).isEqualTo("CANCELLED");assertThatThrownBy(()->reserve(e)).isInstanceOf(com.eventticketing.commerce.api.ApiException.class);doNothing().when(admissionFence).freeze(e.id());cancellation.complete(e.id());assertThat(repo.get(e.id()).status()).isEqualTo("CANCELLED");assertThat(repo.get(e.id()).cancellationPending()).isFalse();cancellation.complete(e.id());assertThat(db.queryForObject("select count(*) from event_commerce.event_audit where event_id=? and action='CANCELLED'",Integer.class,e.id())).isEqualTo(1);}

 @Test void commissionSnapshotIsPersistedAndDatabaseRejectsMutation(){var o=reserve(event(3,1000));String original=db.queryForObject("select commission_snapshot::text from event_commerce.orders where id=?",String.class,o.id());assertThat(original).contains("platformFeeVnd");assertThatThrownBy(()->db.update("update event_commerce.orders set commission_snapshot=jsonb_set(commission_snapshot,'{platformFeeVnd}','0') where id=?",o.id())).hasMessageContaining("immutable");assertThat(db.queryForObject("select commission_snapshot::text from event_commerce.orders where id=?",String.class,o.id())).isEqualTo(original);}
 @Test void eventDecisionAndCancellationCreateDurableMailWithoutDuplication(){var e=event(3,1000);reserve(e);assertThat(db.queryForObject("select count(*) from event_commerce.notification_outbox where payload->>'eventId'=? and payload->>'kind'='APPROVED'",Integer.class,e.id().toString())).isEqualTo(1);events.command(org,e.id(),actor,repo.get(e.id()).version(),"cancel","Fixture cancelled",false);assertThat(db.queryForObject("select count(*) from event_commerce.notification_outbox where payload->>'eventId'=? and payload->>'kind'='CANCELLED'",Integer.class,e.id().toString())).isEqualTo(1);}
 @Autowired Categories categories;
 @Test void inactiveCategoryPreservesHistoryButCannotBeNewSelection(){String name="Category "+UUID.randomUUID().toString().substring(0,8);var category=categories.create(name,actor);UUID id=(UUID)category.get("id");categories.requireNewSelection(name,"");categories.change(id,0,false,actor,"Close selection");categories.requireNewSelection(name,name);assertThatThrownBy(()->categories.requireNewSelection(name,"")).hasMessageContaining("Danh mục");assertThat(categories.active()).doesNotContain(name);}

 @Autowired com.eventticketing.commerce.api.NotificationFeed mailFeed;
 @Test void reminderRechecksWindowConfirmedOrderStartAndCancellation(){var e=event(4,0);var o=reserve(e);assertThat(mailFeed.check(e.id(),actor,24,"local-service-token").get("eligible")).isEqualTo(false);orders.command(o.id(),actor,UUID.randomUUID().toString(),"confirm-free");assertThat(mailFeed.check(e.id(),actor,24,"local-service-token").get("eligible")).isEqualTo(true);assertThat(mailFeed.check(e.id(),actor,1,"local-service-token").get("eligible")).isEqualTo(false);db.update("update event_commerce.events set cancellation_pending=true where id=?",e.id());assertThat(mailFeed.check(e.id(),actor,24,"local-service-token").get("eligible")).isEqualTo(false);db.update("update event_commerce.events set cancellation_pending=false,start_at=now()-interval '1 second' where id=?",e.id());assertThat(mailFeed.check(e.id(),actor,24,"local-service-token").get("eligible")).isEqualTo(false);}
 @Test void incompleteSnapshotCannotPassDatabaseCheckAsSqlUnknown(){var o=reserve(event(3,1000));for(String expression:List.of("commission_snapshot-'grossVnd'","jsonb_set(commission_snapshot,'{platformFeeVnd}','null')","jsonb_set(commission_snapshot,'{configId}','null')")){assertThatThrownBy(()->db.update("insert into event_commerce.orders(id,customer_id,event_id,organization_id,event_name,terms_snapshot,status,total_vnd,expires_at,commission_snapshot) select ?,customer_id,event_id,organization_id,event_name,terms_snapshot,status,total_vnd,expires_at,"+expression+" from event_commerce.orders where id=?",UUID.randomUUID(),o.id())).hasMessageContaining("commission_snapshot_shape");}db.update("insert into event_commerce.orders(id,customer_id,event_id,organization_id,event_name,terms_snapshot,status,total_vnd,expires_at,commission_snapshot) select ?,customer_id,event_id,organization_id,event_name,terms_snapshot,status,total_vnd,expires_at,null from event_commerce.orders where id=?",UUID.randomUUID(),o.id());}
 @Test void mixedFreeAndPaidOrderConfirmsEveryUnitForOnlyThePaidTotal() {
  // BRS-T17: the free line is part of the same atomic allocation and fulfillment fact.
  var e=event(4,1000,4);var paid=e.ticketTypes().stream().filter(t->t.price()>0).findFirst().orElseThrow();
  var free=e.ticketTypes().stream().filter(t->t.price()==0).findFirst().orElseThrow();
  var selection=new Reserve(e.id(),List.of(new Line(paid.id(),2),new Line(free.id(),3)),orders.quote(e.id()).selectionVersion(),true);
  var o=orders.reserve(actor,UUID.randomUUID().toString(),selection);
  assertThat(o.totalVnd()).isEqualTo(2000);
  var claim=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));
  assertThat(claim.order().totalVnd()).isEqualTo(2000);
  var payment=evidence(o,claim.attemptId(),2000,"SUCCESS");
  orders.apply(UUID.randomUUID(),payment);orders.apply(UUID.randomUUID(),payment);
  assertThat(orderRepo.get(o.id()).status()).isEqualTo("CONFIRMED");
  assertThat(repo.get(e.id()).ticketTypes()).allSatisfy(t->assertThat(t.reserved()).isZero());
  assertThat(repo.get(e.id()).ticketTypes().stream().mapToInt(t->t.sold()).sum()).isEqualTo(5);
  assertThat(db.queryForObject("select count(*) from event_commerce.event_outbox where aggregate_id=? and topic='event.order.confirmed.v1'",Integer.class,o.id())).isEqualTo(1);
  assertThat(orderRepo.get(o.id()).items().stream().mapToInt(Item::quantity).sum()).isEqualTo(5);
 }

 @Test void heldOrderKeepsItsPriceAfterPublishedTicketPriceChanges() {
  // BRS-T15: change future pricing through the owner API without repricing the hold.
  var e=event(4,1000);var o=reserve(e);var t=repo.get(e.id()).ticketTypes().getFirst();
  events.ticket(org,e.id(),actor,t.version(),t.id(),new TicketInput(t.name(),t.description(),2000L,t.quota(),t.saleStart(),t.saleEnd(),t.maxPerOrder()));
  assertThat(orders.read(o.id(),actor,false).totalVnd()).isEqualTo(1000);
  assertThat(orderRepo.get(o.id()).items().getFirst().unitPrice()).isEqualTo(1000);
  assertThat(reserve(repo.get(e.id())).totalVnd()).isEqualTo(2000);
 }

 @Test void lockedBuyerDoesNotDiscardVerifiedFundsFromAnExistingAttempt() {
  // BRS-T42: reconciliation is independent of the customer's later session state.
  var e=event(2,1000);var o=reserve(e);var claim=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));
  when(identity.securityState(actor)).thenReturn(new IdentityClient.SecurityState(actor,"LOCKED",true,Set.of("CUSTOMER")));
  orders.apply(UUID.randomUUID(),evidence(o,claim.attemptId(),1000,"SUCCESS"));
  assertThat(orderRepo.get(o.id()).status()).isEqualTo("CONFIRMED");
  assertThat(repo.get(e.id()).ticketTypes().getFirst().sold()).isEqualTo(1);
  assertThatThrownBy(()->reserve(e)).isInstanceOf(com.eventticketing.commerce.api.ApiException.class);
 }

 @Test void confirmedOrderCannotBeCancelledOrReleaseSoldInventory() {
  // BRS-T49: customers cannot turn a confirmed sale back into available stock.
  var e=event(2,0);var o=reserve(e);orders.command(o.id(),actor,UUID.randomUUID().toString(),"confirm-free");
  assertThatThrownBy(()->orders.command(o.id(),actor,UUID.randomUUID().toString(),"cancel")).isInstanceOf(com.eventticketing.commerce.api.ApiException.class);
  assertThat(orderRepo.get(o.id()).status()).isEqualTo("CONFIRMED");
  var stock=repo.get(e.id()).ticketTypes().getFirst();assertThat(stock.sold()).isEqualTo(1);assertThat(stock.reserved()).isZero();
 }

 @Test void simultaneousPaymentFactsConfirmOneOrderAndPublishOneFulfillment() throws Exception {
  // BRS-T20: distinct delivery identities for one collection arrive together.
  var e=event(2,1000);var o=reserve(e);var claim=orders.claim(o.id(),new Claim(actor,UUID.randomUUID().toString()));
  var payment=evidence(o,claim.attemptId(),1000,"SUCCESS");
  var ready=new CountDownLatch(12);var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(12)) {
   var futures=new ArrayList<Future<?>>();
   for(int i=0;i<12;i++) futures.add(pool.submit(()->{
    ready.countDown();if(!start.await(10,TimeUnit.SECONDS))throw new AssertionError("BRS-T20 start barrier");
    orders.apply(UUID.randomUUID(),payment);return null;
   }));
   assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();start.countDown();
   for(var future:futures)future.get(20,TimeUnit.SECONDS);
  } finally {start.countDown();}
  assertThat(orderRepo.get(o.id()).status()).isEqualTo("CONFIRMED");
  var stock=repo.get(e.id()).ticketTypes().getFirst();assertThat(stock.sold()).isEqualTo(1);assertThat(stock.reserved()).isZero();
  assertThat(db.queryForObject("select count(*) from event_commerce.event_outbox where aggregate_id=? and topic='event.order.confirmed.v1'",Integer.class,o.id())).isEqualTo(1);
 }
}
