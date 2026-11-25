import { BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { OrganizerApplicationPageComponent } from './application-page.component';
import { AdminApplicationsPageComponent } from './admin-applications.component';
import { MembersComponent } from './members.component';
import { PayoutsComponent } from './payouts.component';
import { EventsComponent } from './events.component';
import { OrganizationsPageComponent } from './organizations-page.component';
import { OrganizationDetailComponent } from './organization-detail.component';
import { StaffEventsComponent } from './staff-events.component';
import { Application, Organization, Member, Payout, statusLabel } from './organization-api';
import { SessionStore } from '@event-ticketing/session';

describe('Organization workflows and recoverable states',()=>{
 let http:HttpTestingController;
 const org:Organization={id:'org',name:'Test organization',status:'ACTIVE',createdByUserId:'owner',contactEmail:'org@example.test',address:'Test address',description:'Test',version:0,permission:'MANAGEMENT',owner:true};
 const application:Application={id:'application',applicantUserId:'owner',organizationName:'Test organization',contactEmail:'org@example.test',address:'Test address',description:'Test',documentId:'doc',organizationId:null,status:'REJECTED',rejectionReason:'Correct address',version:0,decidedByUserId:null,decidedAt:null,createdAt:''};
 const member:Member={userId:'staff',email:'staff@example.test',displayName:'Staff',permission:'CHECKIN',status:'ACTIVE',owner:false};
 const payout:Payout={id:'payout',version:1,bankName:'Test bank',accountHolder:'Owner',maskedAccount:'•••• 1234',status:'PENDING_REVERIFY',reviewReason:null};
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({id:'org'})},queryParamMap:new BehaviorSubject(convertToParamMap({}))}}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 const fail=(url:string,status=503)=>{const request=http.expectOne(url);const error={code:status===404?'RESOURCE_NOT_FOUND':'TEMPORARY_FAILURE'};request.flush(request.request.responseType==='blob'?new Blob([JSON.stringify(error)]):error,{status,statusText:'Failure'});};
 const appUrl='/api/v1/organizer-applications';const orgUrl='/api/v1/organizations/org';
 function applicationPage(existing:Application|null=application){const f=TestBed.createComponent(OrganizerApplicationPageComponent);existing?http.expectOne(appUrl+'/me').flush(existing):fail(appUrl+'/me',404);f.detectChanges();return f;}
 function members(){const f=TestBed.createComponent(MembersComponent);f.componentRef.setInput('organization',org);f.detectChanges();http.expectOne(orgUrl+'/members').flush([member]);return f;}
 function payouts(){const f=TestBed.createComponent(PayoutsComponent);f.componentRef.setInput('organization',org);f.detectChanges();http.expectOne(orgUrl+'/payout-destinations').flush([payout]);return f;}
 function events(){const f=TestBed.createComponent(EventsComponent);f.componentRef.setInput('organization',org);f.detectChanges();flushEvents();return f;}
 function flushEvents(){http.expectOne(orgUrl+'/events').flush([{id:'event',organizationId:'org',name:'Draft',status:'DRAFT'}]);http.expectOne(orgUrl+'/members').flush([member]);http.expectOne(orgUrl+'/event-staff').flush([{id:'assignment',organizationId:'org',eventId:'event',userId:'staff',status:'ACTIVE'}]);}
 it('preserves application data after failure and makes submitted applications read-only',()=>{
  const c=applicationPage(null).componentInstance;expect(c.error()).toBe('');c.data={...application,description:application.description??'',documentId:application.documentId??''};c.submit();c.submit();fail(appUrl);expect(c.busy()).toBeFalse();expect(c.data.address).toBe(application.address);
  c.submit();http.expectOne(appUrl).flush({...application,status:'PENDING_VERIFICATION'});expect(c.readOnly()).toBeTrue();c.submit();http.expectNone(appUrl);expect(c.message()).toContain('Đã gửi');
 });
 it('saves rejected corrections before resubmitting and exposes resubmit conflicts',()=>{
  const c=applicationPage().componentInstance;c.resubmit();http.expectOne(appUrl+'/application').flush(application);fail(appUrl+'/application/resubmit',409);expect(c.busy()).toBeFalse();expect(c.application()?.status).toBe('REJECTED');
  c.resubmit();http.expectOne(appUrl+'/application').flush(application);http.expectOne(appUrl+'/application/resubmit').flush({...application,status:'PENDING_VERIFICATION'});expect(c.readOnly()).toBeTrue();expect(c.message()).toContain('Đã gửi lại');
 });
 it('saves a rejected draft without submitting it',()=>{const c=applicationPage().componentInstance;c.submit();http.expectOne(appUrl+'/application').flush(application);expect(c.message()).toContain('Đã lưu');expect(c.readOnly()).toBeFalse();});
 it('rejects invalid documents locally and permits upload retry',()=>{
  const c=applicationPage(null).componentInstance;const input=document.createElement('input');const change=(file:File)=>{Object.defineProperty(input,'files',{value:[file],configurable:true});c.upload({target:input} as unknown as Event);};
  c.upload({target:input} as unknown as Event);change(new File(['text'],'proof.txt',{type:'text/plain'}));expect(c.error()).toContain('PDF');http.expectNone('/api/v1/verification-documents');
  change(new File(['%PDF-1.4'],'proof.pdf',{type:'application/pdf'}));fail('/api/v1/verification-documents');expect(c.busy()).toBeFalse();
  change(new File(['%PDF-1.4'],'proof.pdf',{type:'application/pdf'}));http.expectOne('/api/v1/verification-documents').flush({id:'uploaded'});expect(c.data.documentId).toBe('uploaded');
 });
 it('reports application and document-download failures',()=>{const c=applicationPage().componentInstance;c.load();fail(appUrl+'/me');expect(c.loading()).toBeFalse();expect(c.error()).toContain('thử lại');c.download();fail('/api/v1/verification-documents/doc');expect(c.busy()).toBeFalse();});
 it('downloads an authorized document through a blob URL',()=>{const c=applicationPage().componentInstance;spyOn(URL,'createObjectURL').and.returnValue('blob:fixture');spyOn(HTMLAnchorElement.prototype,'click');c.download();http.expectOne('/api/v1/verification-documents/doc').flush(new Blob(['%PDF-1.4']));expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();});
 it('admin decisions refresh displayed status and keep failure recoverable',()=>{
  const c=TestBed.createComponent(AdminApplicationsPageComponent).componentInstance;http.expectOne('/api/v1/admin/organizer-applications').flush([{...application,status:'PENDING_VERIFICATION'}]);c.select({...application,status:'PENDING_VERIFICATION'});c.reason='Correction';c.decide(false);c.confirmation.resolve(true);fail('/api/v1/admin/organizer-applications/application/reject');expect(c.busy()).toBeFalse();
  c.decide(false);c.confirmation.resolve(true);http.expectOne('/api/v1/admin/organizer-applications/application/reject').flush(application);http.expectOne('/api/v1/admin/organizer-applications').flush([application]);expect(c.selected()?.status).toBe('REJECTED');expect(c.message()).toContain('từ chối');
  c.select({...application,status:'PENDING_VERIFICATION'});c.decide(true);c.confirmation.resolve(true);http.expectOne('/api/v1/admin/organizer-applications/application/approve').flush(org);http.expectOne('/api/v1/admin/organizer-applications').flush([{...application,status:'APPROVED'}]);expect(c.message()).toContain('phê duyệt');
 });
 it('admin queue and document failures are visible',()=>{const c=TestBed.createComponent(AdminApplicationsPageComponent).componentInstance;fail('/api/v1/admin/organizer-applications');expect(c.error()).toContain('thử lại');c.decide(true);http.expectNone('/api/v1/admin/organizer-applications/application/approve');c.download('doc');fail('/api/v1/verification-documents/doc');expect(c.busy()).toBeFalse();});
 it('member retry preserves email and revocation requires confirmation',()=>{
  const c=members().componentInstance;c.email=member.email;c.add();c.add();fail(orgUrl+'/members');expect(c.email).toBe(member.email);expect(c.busy()).toBeFalse();c.add();http.expectOne(orgUrl+'/members').flush([member]);expect(c.email).toBe('');
  const confirm=spyOn(window,'confirm').and.returnValue(false);c.revoke(member);http.expectNone(orgUrl+'/members/staff');confirm.and.returnValue(true);c.revoke(member);fail(orgUrl+'/members/staff');expect(c.members()[0].status).toBe('ACTIVE');c.revoke(member);http.expectOne(orgUrl+'/members/staff').flush(null);expect(c.members()[0].status).toBe('REVOKED');
 });
 it('membership list failures permit reload',()=>{const c=members().componentInstance;c.load();fail(orgUrl+'/members');expect(c.busy()).toBeFalse();c.load();http.expectOne(orgUrl+'/members').flush([]);expect(c.members()).toEqual([]);});
 it('payout retry preserves input and success clears the full account number',()=>{
  const f=payouts();const c=f.componentInstance;c.data={bankName:'Bank',accountHolder:'Owner',accountNumber:'12345678901234'};const confirm=spyOn(window,'confirm').and.returnValue(false);c.add();http.expectNone(orgUrl+'/payout-destinations');confirm.and.returnValue(true);c.add();fail(orgUrl+'/payout-destinations');expect(c.data.accountNumber).toBe('12345678901234');c.add();http.expectOne(orgUrl+'/payout-destinations').flush([payout]);f.detectChanges();expect(c.data.accountNumber).toBe('');expect(f.nativeElement.textContent).not.toContain('12345678901234');expect(f.nativeElement.textContent).toContain('•••• 1234');
 });
 it('payout review requires a note and preserves pending state on failure',()=>{
  const c=payouts().componentInstance;c.review(payout,true);http.expectNone('/api/v1/admin/organizations/org/payout-destinations/payout/approve');c.reason='Verified';c.review(payout,true);c.confirmation.resolve(true);fail('/api/v1/admin/organizations/org/payout-destinations/payout/approve');expect(c.payouts()[0].status).toBe('PENDING_REVERIFY');c.review(payout,false);c.confirmation.resolve(true);http.expectOne('/api/v1/admin/organizations/org/payout-destinations/payout/reject').flush([{...payout,status:'REJECTED'}]);expect(c.payouts()[0].status).toBe('REJECTED');expect(c.reason).toBe('');
 });
 it('payout list failure clears loading',()=>{const c=payouts().componentInstance;c.load();fail(orgUrl+'/payout-destinations');expect(c.busy()).toBeFalse();});
 it('creates drafts, handles failure, assigns staff and refreshes server state',()=>{
  const navigate=spyOn(TestBed.inject(Router),'navigate').and.resolveTo(true);
  const c=events().componentInstance;c.name='Draft new';c.create();fail(orgUrl+'/events');expect(c.name).toBe('Draft new');expect(c.hasUnsavedChanges()).toBeTrue();expect(navigate).not.toHaveBeenCalled();c.create();http.expectOne(orgUrl+'/events').flush({id:'created',name:'Draft new',organizationId:'org',status:'DRAFT'});expect(c.eventId).toBe('');expect(c.name).toBe('');expect(c.hasUnsavedChanges()).toBeFalse();expect(navigate).toHaveBeenCalledOnceWith(['/organizations','org','events','created']);
  c.eventId='created';c.userId='staff';c.assign();fail(orgUrl+'/events/created/staff/staff');expect(c.busy()).toBeFalse();c.assign();http.expectOne(orgUrl+'/events/created/staff/staff').flush(null);flushEvents();expect(c.message()).toContain('Đã phân công');
  expect(c.eventName('event')).toBe('Draft');expect(c.staffName('staff')).toBe(member.email);expect(c.eventName('missing')).toBe('missing');expect(c.staffName('missing')).toBe('missing');
 });
 it('keeps genuine pending staff input when creating a draft',()=>{
  spyOn(TestBed.inject(Router),'navigate').and.resolveTo(false);
  const c=events().componentInstance;c.eventId='event';c.userId='staff';c.name='Another event';c.create();
  http.expectOne(orgUrl+'/events').flush({id:'created',name:'Another event',organizationId:'org',status:'DRAFT'});
  expect(c.name).toBe('');expect(c.eventId).toBe('event');expect(c.userId).toBe('staff');expect(c.hasUnsavedChanges()).toBeTrue();
 });
 it('does not submit blank event names or create twice while saving',()=>{
  const c=events().componentInstance;c.name='   ';c.create();http.expectNone(orgUrl+'/events');
  c.name='New event';c.create();c.create();fail(orgUrl+'/events');expect(c.name).toBe('New event');expect(c.busy()).toBeFalse();
 });
 it('retains assignment status when revocation fails',()=>{
  const c=events().componentInstance;const assignment=c.assignments()[0];const confirm=spyOn(window,'confirm').and.returnValue(false);c.revoke(assignment);http.expectNone(orgUrl+'/events/event/staff/staff');confirm.and.returnValue(true);c.revoke(assignment);fail(orgUrl+'/events/event/staff/staff');expect(c.assignments()[0].status).toBe('ACTIVE');c.revoke(assignment);http.expectOne(orgUrl+'/events/event/staff/staff').flush(null);expect(c.assignments()[0].status).toBe('REVOKED');
 });
 it('event-list failure is visible and clears loading',()=>{const c=events().componentInstance;c.load();fail(orgUrl+'/events');expect(http.expectOne(orgUrl+'/members').cancelled).toBeTrue();expect(http.expectOne(orgUrl+'/event-staff').cancelled).toBeTrue();expect(c.busy()).toBeFalse();});
 it('organization listing recovers from an API failure',()=>{const c=TestBed.createComponent(OrganizationsPageComponent).componentInstance;fail('/api/v1/organizations');expect(c.busy()).toBeFalse();c.load();http.expectOne('/api/v1/organizations').flush([org]);expect(c.organizations()[0].id).toBe('org');expect(c.error()).toBe('');});
 it('settings preserve edits on conflict and adopt returned versions',()=>{
  const c=TestBed.createComponent(OrganizationDetailComponent).componentInstance;http.expectOne(orgUrl).flush(org);c.data.name='Changed';c.save();c.save();fail(orgUrl,409);expect(c.data.name).toBe('Changed');c.save();http.expectOne(orgUrl).flush({...org,name:'Changed',version:1});expect(c.data.version).toBe(1);expect(c.message()).toContain('Đã lưu');
 });
 it('suspended and unprivileged organization settings are disabled',()=>{
  const f=TestBed.createComponent(OrganizationDetailComponent);http.expectOne(orgUrl).flush({...org,status:'SUSPENDED'});f.detectChanges();expect(f.nativeElement.querySelector('fieldset').disabled).toBeTrue();
  f.componentInstance.receive({...org,permission:'CHECKIN',owner:false});f.detectChanges();expect(f.componentInstance.canManage()).toBeFalse();expect(f.componentInstance.canFinance()).toBeFalse();
  f.componentInstance.receive({...org,permission:'FINANCE',owner:false});expect(f.componentInstance.canFinance()).toBeTrue();
 });
 it('admin status changes require reasons and show server errors',()=>{
  TestBed.inject(SessionStore).setSession('token',{id:'admin',email:'admin@example.test',displayName:'Admin',roles:['ADMIN'],status:'ACTIVE',emailVerified:true,preferredLocale:'vi',createdAt:''});
  const c=TestBed.createComponent(OrganizationDetailComponent).componentInstance;http.expectOne(orgUrl).flush(org);expect(c.admin()).toBeTrue();c.status();http.expectNone('/api/v1/admin/organizations/org/suspend');c.reason='Review';c.status();c.confirmation.resolve(true);fail('/api/v1/admin/organizations/org/suspend');expect(c.organization()?.status).toBe('ACTIVE');c.status();c.confirmation.resolve(true);http.expectOne('/api/v1/admin/organizations/org/suspend').flush({...org,status:'SUSPENDED'});expect(c.reason).toBe('');c.reason='Reviewed';c.status();c.confirmation.resolve(true);http.expectOne('/api/v1/admin/organizations/org/restore').flush(org);expect(c.organization()?.status).toBe('ACTIVE');
 });
 it('shows audit entries and read errors without unrelated data',()=>{
  const c=TestBed.createComponent(OrganizationDetailComponent).componentInstance;fail(orgUrl,404);expect(c.organization()).toBeNull();c.load();http.expectOne(orgUrl).flush(org);(TestBed.inject(ActivatedRoute).queryParamMap as BehaviorSubject<ReturnType<typeof convertToParamMap>>).next(convertToParamMap({section:'audit'}));fail(orgUrl+'/audit');expect(c.error()).toContain('thử lại');(TestBed.inject(ActivatedRoute).queryParamMap as BehaviorSubject<ReturnType<typeof convertToParamMap>>).next(convertToParamMap({section:'audit'}));http.expectOne(orgUrl+'/audit').flush([{id:'audit',action:'MEMBER_REVOKED'}]);expect(c.audit()[0].action).toBe('MEMBER_REVOKED');
 });
 it('staff refresh removes revoked events and a stale action surfaces rejection',()=>{
  const c=TestBed.createComponent(StaffEventsComponent).componentInstance;http.expectOne('/api/v1/staff/events').flush([{id:'event',name:'Draft',status:'DRAFT'}]);c.check('event');http.expectOne('/api/v1/staff/events/event').flush({id:'event',name:'Draft'});expect(c.message()).toContain('được phân công');c.check('event');fail('/api/v1/staff/events/event',404);expect(c.error()).toContain('Không tìm thấy');c.load();http.expectOne('/api/v1/staff/events').flush([]);expect(c.events()).toEqual([]);c.load();fail('/api/v1/staff/events');expect(c.busy()).toBeFalse();
 });
 it('translates known states and retains unknown values',()=>{expect(statusLabel('ACTIVE')).toBe('Đang hoạt động');expect(statusLabel('FUTURE_STATE')).toBe('FUTURE_STATE');});
});
