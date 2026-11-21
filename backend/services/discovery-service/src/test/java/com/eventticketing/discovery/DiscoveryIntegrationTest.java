package com.eventticketing.discovery;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
@SpringBootTest(properties={"app.messaging.enabled=false","spring.kafka.admin.auto-create=false"}) @AutoConfigureMockMvc @Testcontainers
class DiscoveryIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired ProjectionService projection;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 String message(UUID id,long version,String state,int sold,boolean featured,String name) throws Exception {
  var event=new LinkedHashMap<String,Object>();event.put("id",id);event.put("organizationId",UUID.randomUUID());event.put("version",version);event.put("status",state);event.put("publishedAt",state.equals("DRAFT")?null:Instant.now());event.put("name",name);event.put("category","Workshop");event.put("city","Hà Nội");event.put("startAt",Instant.now().plusSeconds(86400));event.put("endAt",Instant.now().plusSeconds(90000));event.put("updatedAt",Instant.now());event.put("featured",featured);event.put("reviewReason","Private moderation note");
  event.put("ticketTypes",List.of(Map.of("id",UUID.randomUUID(),"price",100000,"sold",sold,"available",5,"paused",false,"saleStart",Instant.now().minusSeconds(100),"saleEnd",Instant.now().plusSeconds(86000))));
  return json.writeValueAsString(Map.of("eventId",UUID.randomUUID(),"eventVersion",1,"producer","event-commerce","aggregateId",id,"payload",event));
 }
 @Test void privateAndCancelledCannotLeakOrBeResurrected() throws Exception {
  UUID id=UUID.randomUUID();String name="Scoped "+id;projection.apply(message(id,1,"DRAFT",0,false,name));mvc.perform(get("/api/v1/discover/events/"+id)).andExpect(status().isNotFound());
  String published=message(id,2,"PUBLISHED",0,true,name);projection.apply(published);projection.apply(published);
  mvc.perform(get("/api/v1/discover/events/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.reviewReason").doesNotExist()).andExpect(jsonPath("$.ticketTypes[0].saleState").value("ON_SALE"));
  projection.apply(message(id,3,"CANCELLED",0,false,name));projection.apply(published);
  mvc.perform(get("/api/v1/discover/events/"+id)).andExpect(jsonPath("$.ticketTypes[0].saleState").value("UNAVAILABLE"));
  mvc.perform(get("/api/v1/discover/events").param("q",name)).andExpect(jsonPath("$.data").isEmpty());
 }

 @Test void filtersPaginationAndPopularityUseRealProjectionValues() throws Exception {
  String prefix="Search "+UUID.randomUUID();UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();projection.apply(message(a,1,"PUBLISHED",0,false,prefix+" A"));projection.apply(message(b,1,"PUBLISHED",10,true,prefix+" B"));projection.apply(message(c,1,"PUBLISHED",2,false,prefix+" C"));
  String body=mvc.perform(get("/api/v1/discover/events").param("q",prefix).param("sort","popular").param("limit","1")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(b.toString())).andExpect(jsonPath("$.hasMore").value(true)).andReturn().getResponse().getContentAsString();
  String cursor=json.readTree(body).get("nextCursor").asText();mvc.perform(get("/api/v1/discover/events").param("q",prefix).param("sort","popular").param("limit","1").param("cursor",cursor)).andExpect(jsonPath("$.data[0].id").value(c.toString()));
  mvc.perform(get("/api/v1/discover/events").param("q",prefix).param("featured","true").param("category","Workshop").param("city","Hà Nội").param("minPrice","100000").param("maxPrice","100000")).andExpect(jsonPath("$.data.length()").value(1));
  mvc.perform(get("/api/v1/discover/events").param("q",prefix).param("maxPrice","99999")).andExpect(jsonPath("$.data").isEmpty());
  mvc.perform(get("/api/v1/discover/events").param("q",prefix).param("from",Instant.now().plusSeconds(200000).toString())).andExpect(jsonPath("$.data").isEmpty());
  mvc.perform(get("/api/v1/discover/events").param("cursor","malformed")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/discover/events").param("minPrice","100").param("maxPrice","0")).andExpect(status().isBadRequest());
 }
 @Test void invalidEnvelopeRollsBackInboxAndCanBeRetried() throws Exception {
  UUID id=UUID.randomUUID();String body=message(id,1,"PUBLISHED",0,false,"Malformed");var node=json.readTree(body);((com.fasterxml.jackson.databind.node.ObjectNode)node.get("payload")).put("startAt","broken");
  assertThatThrownBy(()->projection.apply(node.toString())).isInstanceOf(RuntimeException.class);assertThat(db.queryForObject("select count(*) from discovery.inbox where event_id=?",Integer.class,UUID.fromString(node.get("eventId").asText()))).isZero();projection.apply(body);assertThat(db.queryForObject("select count(*) from discovery.public_events where id=?",Integer.class,id)).isEqualTo(1);
 }
 @Test void saleStateClosesOnServerClockWithoutAnotherMessage() throws Exception {
  UUID id=UUID.randomUUID();var node=json.readTree(message(id,1,"PUBLISHED",0,false,"Past sale"));((com.fasterxml.jackson.databind.node.ObjectNode)node.get("payload").get("ticketTypes").get(0)).put("saleEnd",Instant.now().minusSeconds(1).toString());projection.apply(node.toString());
  mvc.perform(get("/api/v1/discover/events/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.ticketTypes[0].saleState").value("CLOSED"));
 }

 @Autowired AttendeeProjection attendees;
 @Test void attendeesAreScopedReadModelsWithDeduplicationAndPagination() throws Exception {
  UUID event=UUID.randomUUID(),ticket=UUID.randomUUID(),message=UUID.randomUUID();
  var payload=new LinkedHashMap<String,Object>();payload.put("ticketId",ticket);payload.put("eventId",event);payload.put("version",1);payload.put("attendeeName","Synthetic attendee");payload.put("attendeeEmail","attendee@example.test");payload.put("ticketTypeName","Community");payload.put("status","ISSUED");
  var envelope=new LinkedHashMap<String,Object>();envelope.put("eventId",message);envelope.put("eventVersion",1);envelope.put("producer","ticket-admission");envelope.put("payload",payload);String issued=json.writeValueAsString(envelope);
  attendees.apply(issued);attendees.apply(issued);assertThat((List<?>)attendees.list(event,null,50).get("data")).hasSize(1);assertThat((List<?>)attendees.list(UUID.randomUUID(),null,50).get("data")).isEmpty();
  envelope.put("eventId",UUID.randomUUID());payload.put("version",2);payload.put("status","VOIDED");attendees.apply(json.writeValueAsString(envelope));attendees.apply(issued);
  assertThat(db.queryForObject("select status from discovery.event_attendees where ticket_id=?",String.class,ticket)).isEqualTo("VOIDED");
  mvc.perform(get("/internal/events/"+event+"/attendees")).andExpect(status().isUnauthorized());
  mvc.perform(get("/internal/events/"+event+"/attendees").header("X-Service-Token","local-service-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("Synthetic attendee"));
 }
}
