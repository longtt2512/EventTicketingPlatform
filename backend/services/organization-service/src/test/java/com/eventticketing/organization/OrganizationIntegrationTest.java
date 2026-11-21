package com.eventticketing.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.eventticketing.organization.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrganizationIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
  r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);
  r.add("app.data-encryption-key",()->Base64.getEncoder().encodeToString(new byte[32]));
 }
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 @MockitoBean IdentityClient identity;@MockitoBean CommerceClient commerce;@MockitoBean DocumentStorage storage;@MockitoBean JwtDecoder decoder;
 final UUID owner=UUID.randomUUID(),admin=UUID.randomUUID(),other=UUID.randomUUID();
 @BeforeEach void setup(){
  when(identity.securityState(any())).thenAnswer(a->new IdentityClient.SecurityState(a.getArgument(0),"ACTIVE",true,Set.of("CUSTOMER")));
  when(storage.get(anyString())).thenReturn("%PDF-1.4 test".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
 }
 MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder r,UUID user,boolean isAdmin){return r.with(jwt().jwt(j->j.subject(user.toString())).authorities(new SimpleGrantedAuthority(isAdmin?"ROLE_ADMIN":"ROLE_CUSTOMER")));}
 MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder r,Object value) throws Exception {return r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(value));}
 JsonNode read(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}
 UUID upload(UUID actor)throws Exception {
  var file=new MockMultipartFile("file","proof.pdf","application/pdf","%PDF-1.4 test".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  return UUID.fromString(read(mvc.perform(multipart("/api/v1/verification-documents").file(file).with(jwt().jwt(j->j.subject(actor.toString())))).andExpect(status().isCreated()).andReturn()).get("id").asText());
 }
 Map<String,Object> application(UUID doc){return Map.of("organizationName","Organization "+UUID.randomUUID(),"contactEmail","org@example.test","address","123 Test Road","description","Verification fixture","documentId",doc);}
 UUID createOrg(UUID actor){UUID org=UUID.randomUUID();db.update("insert into organization.organizations(id,name,created_by_user_id) values (?,?,?)",org,"Test org",actor);db.update("insert into organization.organization_members(organization_id,user_id) values (?,?)",org,actor);return org;}
 void member(UUID org,UUID user,String permission){db.update("insert into organization.organization_members(organization_id,user_id,permission) values (?,?,?)",org,user,permission);}
 void event(UUID event,UUID org){when(commerce.event(event)).thenReturn(new CommerceClient.Event(event,org,"Draft event","DRAFT"));}
 @Test void documentValidationOwnershipAndAdminReviewRestriction() throws Exception {
  UUID document=upload(owner);
  mvc.perform(as(get("/api/v1/verification-documents/"+document),other,false)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  mvc.perform(as(get("/api/v1/verification-documents/"+document),admin,true)).andExpect(status().isNotFound());
  mvc.perform(as(get("/api/v1/verification-documents/"+document),owner,false)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  mvc.perform(multipart("/api/v1/verification-documents").file(new MockMultipartFile("file","x.pdf","application/pdf","<script>bad</script>".getBytes())).with(jwt().jwt(j->j.subject(owner.toString())))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DOCUMENT"));
  mvc.perform(as(body(post("/api/v1/organizer-applications"),application(document)),other,false)).andExpect(status().isNotFound());
  mvc.perform(as(body(post("/api/v1/organizer-applications"),application(document)),owner,false)).andExpect(status().isCreated());
  mvc.perform(as(get("/api/v1/verification-documents/"+document),admin,true)).andExpect(status().isOk());
 }
 @Test void rejectedApplicationCanBeCorrectedAndApprovedButPendingCannotBeEdited() throws Exception {
  var request=application(upload(owner));
  String id=read(mvc.perform(as(body(post("/api/v1/organizer-applications"),request),owner,false)).andExpect(status().isCreated()).andReturn()).get("id").asText();
  mvc.perform(as(body(patch("/api/v1/organizer-applications/"+id),request),owner,false)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_APPLICATION_STATE"));
  mvc.perform(as(post("/api/v1/admin/organizer-applications/"+id+"/approve"),owner,false)).andExpect(status().isForbidden());
  mvc.perform(as(body(post("/api/v1/admin/organizer-applications/"+id+"/reject"),Map.of("reason","")),admin,true)).andExpect(status().isBadRequest());
  mvc.perform(as(body(post("/api/v1/admin/organizer-applications/"+id+"/reject"),Map.of("reason","Correct legal name")),admin,true)).andExpect(status().isOk());
  mvc.perform(as(body(patch("/api/v1/organizer-applications/"+id),request),other,false)).andExpect(status().isNotFound());
  mvc.perform(as(body(patch("/api/v1/organizer-applications/"+id),request),owner,false)).andExpect(status().isOk());
  mvc.perform(as(post("/api/v1/organizer-applications/"+id+"/resubmit"),owner,false)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
  var org=read(mvc.perform(as(post("/api/v1/admin/organizer-applications/"+id+"/approve"),admin,true)).andExpect(status().isOk()).andReturn()).get("id").asText();
  mvc.perform(as(post("/api/v1/admin/organizer-applications/"+id+"/approve"),admin,true)).andExpect(status().isConflict());
  mvc.perform(as(get("/api/v1/organizations/"+org),owner,false)).andExpect(status().isOk()).andExpect(jsonPath("$.owner").value(true));
  assertThat(db.queryForObject("select count(*) from organization.organization_audit where application_id=?",Integer.class,UUID.fromString(id))).isEqualTo(5);
 }
 @Test void organizationScopePermissionsAndOptimisticConflictAreEnforced() throws Exception {
  UUID org=createOrg(owner);member(org,other,"FINANCE");
  UUID stranger=UUID.randomUUID();
  mvc.perform(as(get("/api/v1/organizations/"+org),stranger,false)).andExpect(status().isNotFound());
  mvc.perform(as(get("/api/v1/organizations/"+org+"/members"),other,false)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PERMISSION_REQUIRED"));
  Map<String,Object> update=Map.of("name","Updated","contactEmail","org@example.test","address","Test Road","description","Test","version",0);
  mvc.perform(as(body(patch("/api/v1/organizations/"+org),update),other,false)).andExpect(status().isForbidden());
  mvc.perform(as(body(patch("/api/v1/organizations/"+org),update),owner,false)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
  mvc.perform(as(body(patch("/api/v1/organizations/"+org),update),owner,false)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
  mvc.perform(as(get("/api/v1/organizations/"+org+"/payout-destinations"),other,false)).andExpect(status().isOk());
 }
 @Test void ownerProtectedAndMembershipCannotGrantAdminOrCrossOrganizationAccess() throws Exception {
  UUID org=createOrg(owner);String email="member@example.test";
  when(identity.lookup(email)).thenReturn(new IdentityClient.MemberLookup(other,email,"Staff"));
  mvc.perform(as(body(post("/api/v1/organizations/"+org+"/members"),Map.of("email",email,"permission","ADMIN")),owner,false)).andExpect(status().isBadRequest());
  mvc.perform(as(body(post("/api/v1/organizations/"+org+"/members"),Map.of("email",email,"permission","CHECKIN")),owner,false)).andExpect(status().isOk());
  mvc.perform(as(get("/api/v1/organizations/"+org),other,false)).andExpect(status().isOk());
  mvc.perform(as(delete("/api/v1/organizations/"+org+"/members/"+owner),owner,false)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OWNER_PROTECTED"));
  mvc.perform(as(delete("/api/v1/organizations/"+org+"/members/"+other),other,false)).andExpect(status().isForbidden());
  mvc.perform(as(delete("/api/v1/organizations/"+org+"/members/"+other),owner,false)).andExpect(status().isNoContent());
  mvc.perform(as(get("/api/v1/organizations/"+org),other,false)).andExpect(status().isNotFound());
  assertThat(db.queryForObject("select count(*) from organization.organization_members where organization_id=? and membership_status='REVOKED'",Integer.class,org)).isEqualTo(1);
 }
 @Test void payoutIsEncryptedMaskedVersionedAndRequiresAdminDecision() throws Exception {
  UUID org=createOrg(owner);member(org,other,"MANAGEMENT");String account="12345678901234";
  Map<String,String> input=Map.of("bankName","Test bank","accountHolder","Test owner","accountNumber",account);
  mvc.perform(as(body(post("/api/v1/organizations/"+org+"/payout-destinations"),input),other,false)).andExpect(status().isForbidden());
  var result=mvc.perform(as(body(post("/api/v1/organizations/"+org+"/payout-destinations"),input),owner,false)).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING_REVERIFY")).andReturn();
  assertThat(result.getResponse().getContentAsString()).doesNotContain(account);String id=read(result).get(0).get("id").asText();
  assertThat(db.queryForObject("select encrypted_details from organization.payout_destinations where id=?",String.class,UUID.fromString(id))).doesNotContain(account);
  mvc.perform(as(body(post("/api/v1/admin/organizations/"+org+"/payout-destinations/"+id+"/approve"),Map.of("reason","Checked")),owner,false)).andExpect(status().isForbidden());
  mvc.perform(as(body(post("/api/v1/admin/organizations/"+org+"/payout-destinations/"+id+"/approve"),Map.of("reason","Checked")),admin,true)).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("ACTIVE"));
  var second=read(mvc.perform(as(body(post("/api/v1/organizations/"+org+"/payout-destinations"),input),owner,false)).andExpect(status().isOk()).andReturn());
  assertThat(second.get(0).get("version").asInt()).isEqualTo(2);assertThat(second.get(1).get("status").asText()).isEqualTo("ACTIVE");
  mvc.perform(as(body(post("/api/v1/admin/organizations/"+org+"/payout-destinations/"+second.get(0).get("id").asText()+"/approve"),Map.of("reason","Replacement checked")),admin,true)).andExpect(status().isOk()).andExpect(jsonPath("$[1].status").value("REVOKED"));
 }
 @Test void staffAssignmentChecksEventOwnershipAndRevocationAndSuspensionImmediately() throws Exception {
  UUID org=createOrg(owner),foreign=createOrg(UUID.randomUUID()),event=UUID.randomUUID(),foreignEvent=UUID.randomUUID();member(org,other,"CHECKIN");event(event,org);event(foreignEvent,foreign);
  String path="/api/v1/organizations/"+org+"/events/"+event+"/staff/"+other;
  mvc.perform(as(put("/api/v1/organizations/"+org+"/events/"+foreignEvent+"/staff/"+other),owner,false)).andExpect(status().isNotFound());
  mvc.perform(as(put(path),owner,false)).andExpect(status().isNoContent());mvc.perform(as(put(path),owner,false)).andExpect(status().isNoContent());
  assertThat(db.queryForObject("select count(*) from organization.event_staff_assignments where event_id=?",Integer.class,event)).isEqualTo(1);
  mvc.perform(as(get("/api/v1/staff/events/"+event),other,false)).andExpect(status().isOk());
  mvc.perform(as(body(post("/api/v1/admin/organizations/"+org+"/suspend"),Map.of("reason","Review")),admin,true)).andExpect(status().isOk());
  mvc.perform(as(get("/api/v1/staff/events/"+event),other,false)).andExpect(status().isNotFound());
  mvc.perform(as(put(path),owner,false)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORGANIZATION_SUSPENDED"));
  mvc.perform(as(body(post("/api/v1/admin/organizations/"+org+"/restore"),Map.of("reason","Verified again")),admin,true)).andExpect(status().isOk());
  mvc.perform(as(get("/api/v1/staff/events/"+event),other,false)).andExpect(status().isOk());
  mvc.perform(as(delete(path),owner,false)).andExpect(status().isNoContent());mvc.perform(as(get("/api/v1/staff/events/"+event),other,false)).andExpect(status().isNotFound());
  mvc.perform(as(put(path),owner,false)).andExpect(status().isNoContent());
  mvc.perform(as(delete("/api/v1/organizations/"+org+"/members/"+other),owner,false)).andExpect(status().isNoContent());
  mvc.perform(as(get("/api/v1/staff/events/"+event),other,false)).andExpect(status().isNotFound());
  assertThat(db.queryForObject("select count(*) from organization.organization_audit where organization_id=?",Integer.class,org)).isGreaterThanOrEqualTo(6);
 }
 @Test void lockedCurrentAccountAndUnauthenticatedInternalRequestsAreRejected() throws Exception {
  UUID session=UUID.randomUUID();var token=Jwt.withTokenValue("signed-token").header("alg","RS256").subject(owner.toString()).claim("sid",session.toString()).build();when(decoder.decode("signed-token")).thenReturn(token);
  when(identity.sessionState(owner,session)).thenReturn(new IdentityClient.SecurityState(owner,"ACTIVE",true,Set.of("CUSTOMER")));
  mvc.perform(get("/api/v1/organizations").header("Authorization","Bearer signed-token")).andExpect(status().isOk());
  when(identity.sessionState(owner,session)).thenReturn(new IdentityClient.SecurityState(owner,"LOCKED",true,Set.of("CUSTOMER")));
  mvc.perform(get("/api/v1/organizations").header("Authorization","Bearer signed-token")).andExpect(status().isUnauthorized());
  mvc.perform(get("/internal/organizations/"+UUID.randomUUID()+"/authorization").param("userId",owner.toString())).andExpect(status().isUnauthorized());
 }
 @Autowired com.eventticketing.organization.application.AdmissionAuthorization admission;
 @Test void admissionChecksLiveMembershipAssignmentOrganizationAndEmail(){UUID org=createOrg(owner),event=UUID.randomUUID();member(org,other,"CHECKIN");event(event,org);assertThatThrownBy(()->admission.authorize(event,other)).isInstanceOf(com.eventticketing.organization.api.ApiException.class);db.update("insert into organization.event_staff_assignments(id,organization_id,event_id,user_id,status,assigned_by) values (?,?,?,?,'ACTIVE',?)",UUID.randomUUID(),org,event,other,owner);admission.authorize(event,other);admission.authorize(event,owner);db.update("update organization.organizations set status='SUSPENDED' where id=?",org);assertThatThrownBy(()->admission.authorize(event,other)).isInstanceOf(com.eventticketing.organization.api.ApiException.class);db.update("update organization.organizations set status='ACTIVE' where id=?",org);admission.authorize(event,other);db.update("update organization.event_staff_assignments set status='REVOKED' where event_id=?",event);assertThatThrownBy(()->admission.authorize(event,other)).isInstanceOf(com.eventticketing.organization.api.ApiException.class);when(identity.securityState(owner)).thenReturn(new IdentityClient.SecurityState(owner,"ACTIVE",false,Set.of("ORGANIZER")));assertThatThrownBy(()->admission.authorize(event,owner)).isInstanceOf(com.eventticketing.organization.api.ApiException.class);}

 @Autowired com.eventticketing.organization.application.FinanceAuthorization finance;
 UUID verifiedPayout(UUID org){UUID payout=UUID.randomUUID();db.update("insert into organization.payout_destinations(id,organization_id,destination_version,bank_name,account_holder,masked_account,encrypted_details,status) values (?,?,1,'Local bank','Fixture','•••• 1234','encrypted-fixture','ACTIVE')",payout,org);return payout;}
 @Test void financeScopeChecksCurrentMembershipAndMasksPayout(){UUID org=createOrg(owner);verifiedPayout(org);member(org,other,"FINANCE");assertThat(finance.scope(org,other,false).toString()).contains("1234").doesNotContain("encrypted-fixture");db.update("update organization.organization_members set membership_status='REVOKED' where organization_id=? and user_id=?",org,other);assertThatThrownBy(()->finance.scope(org,other,false)).isInstanceOf(com.eventticketing.organization.api.ApiException.class);assertThatThrownBy(()->finance.scope(createOrg(admin),owner,false)).isInstanceOf(com.eventticketing.organization.api.ApiException.class);}
 @Test void payoutAuthorizationReplaysOneProofAndRejectsChangedDestinationAndSuspension(){UUID org=createOrg(owner),payout=verifiedPayout(org),settlement=UUID.randomUUID();when(identity.securityState(admin)).thenReturn(new IdentityClient.SecurityState(admin,"ACTIVE",true,Set.of("ADMIN")));var request=new com.eventticketing.organization.application.FinanceAuthorization.Authorize(settlement,admin,payout,1,"evidence-fingerprint");var first=finance.authorize(org,request);db.update("update organization.payout_destinations set status='REVOKED' where id=?",payout);assertThat(finance.authorize(org,request)).isEqualTo(first);assertThatThrownBy(()->finance.authorize(org,new com.eventticketing.organization.application.FinanceAuthorization.Authorize(settlement,admin,UUID.randomUUID(),1,"evidence-fingerprint"))).hasMessageContaining("nội dung khác");assertThatThrownBy(()->finance.authorize(org,new com.eventticketing.organization.application.FinanceAuthorization.Authorize(UUID.randomUUID(),admin,payout,1,"new-evidence"))).hasMessageContaining("Tài khoản");db.update("update organization.organizations set status='SUSPENDED' where id=?",org);assertThatThrownBy(()->finance.scope(org,admin,true)).hasMessageContaining("tạm dừng");}

 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Test void payoutReplacementLockPreventsAuthorizationOfRevokedDestination()throws Exception{
  UUID org=createOrg(owner),payout=verifiedPayout(org);when(identity.securityState(admin)).thenReturn(new IdentityClient.SecurityState(admin,"ACTIVE",true,Set.of("ADMIN")));
  var locked=new java.util.concurrent.CountDownLatch(1);var commit=new java.util.concurrent.CountDownLatch(1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var replacement=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{db.queryForList("select id from organization.organizations where id=? for update",org);db.update("update organization.payout_destinations set status='REVOKED' where id=?",payout);locked.countDown();try{commit.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}return true;}));
   assertThat(locked.await(2,java.util.concurrent.TimeUnit.SECONDS)).isTrue();var attempted=new java.util.concurrent.CountDownLatch(1);
   var authorization=pool.submit(()->{attempted.countDown();return finance.authorize(org,new com.eventticketing.organization.application.FinanceAuthorization.Authorize(UUID.randomUUID(),admin,payout,1,"concurrent-evidence"));});
   assertThat(attempted.await(1,java.util.concurrent.TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->authorization.get(150,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
   commit.countDown();assertThat(replacement.get(3,java.util.concurrent.TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->authorization.get(3,java.util.concurrent.TimeUnit.SECONDS)).hasCauseInstanceOf(com.eventticketing.organization.api.ApiException.class).hasStackTraceContaining("Tài khoản nhận tiền đã đổi");
   assertThat(db.queryForObject("select count(*) from organization.payout_authorizations where organization_id=?",Integer.class,org)).isZero();
  }finally{commit.countDown();}
 }
}
