import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { WORKSPACE } from '@event-ticketing/ui';
import { SessionStore } from '@event-ticketing/session';
import { HomePageComponent } from './home-page.component';
import { RegisterPageComponent } from './register-page.component';
import { User } from '@event-ticketing/models';
const user:User={id:'user',displayName:'QA Staff',email:'staff@example.test',roles:['CUSTOMER'],emailVerified:true,status:'ACTIVE',preferredLocale:'vi',createdAt:''};
function setup(workspace:string){TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:WORKSPACE,useValue:workspace}]});TestBed.inject(SessionStore).setSession('fixture',user);return TestBed.inject(HttpTestingController);}
describe('Portal onboarding',()=>{
 it('explains staff enrollment and refreshes newly granted assignments without logging in again',()=>{
  const http=setup('checkin'),f=TestBed.createComponent(HomePageComponent);http.expectOne('/api/v1/staff/events').flush([]);f.detectChanges();expect(f.nativeElement.textContent).toContain('Chờ cấp quyền soát vé');
  f.componentInstance.refreshAccess();http.expectOne('/api/v1/users/me').flush({...user,roles:['CUSTOMER','CHECKIN_STAFF']});http.expectOne('/api/v1/staff/events').flush([]);f.changeDetectorRef.markForCheck();f.detectChanges();expect(f.nativeElement.textContent).toContain('Chờ phân công sự kiện');
  f.componentInstance.load();http.expectOne('/api/v1/staff/events').flush([{id:'event'}]);f.changeDetectorRef.markForCheck();f.detectChanges();expect(f.nativeElement.textContent).toContain('Bạn đã được phân công sự kiện');http.verify();f.destroy();
 });
 it('shows a pending or rejected organizer application and an explicit retry after service errors',()=>{
  const http=setup('organizer'),f=TestBed.createComponent(HomePageComponent);http.expectOne('/api/v1/organizations').flush([]);http.expectOne('/api/v1/organizer-applications/me').flush({id:'app',organizationName:'QA Org',status:'PENDING_VERIFICATION'});f.detectChanges();expect(f.nativeElement.textContent).toContain('Đang chờ duyệt');
  f.componentInstance.load();http.expectOne('/api/v1/organizations').flush([]);http.expectOne('/api/v1/organizer-applications/me').flush({id:'app',organizationName:'QA Org',status:'REJECTED',rejectionReason:'Bổ sung tài liệu'});f.changeDetectorRef.markForCheck();f.detectChanges();expect(f.nativeElement.textContent).toContain('Bổ sung tài liệu');
  f.componentInstance.load();http.expectOne('/api/v1/organizations').flush([]);http.expectOne('/api/v1/organizer-applications/me').flush(null,{status:503,statusText:'Unavailable'});f.changeDetectorRef.markForCheck();f.detectChanges();expect(f.nativeElement.querySelector('[role=alert]')).not.toBeNull();http.verify();f.destroy();
 });
 it('treats no application as onboarding rather than a broken account',()=>{
  const http=setup('organizer'),f=TestBed.createComponent(HomePageComponent);http.expectOne('/api/v1/organizations').flush([]);http.expectOne('/api/v1/organizer-applications/me').flush(null,{status:404,statusText:'Not Found'});f.detectChanges();expect(f.nativeElement.textContent).toContain('Bắt đầu với vai trò nhà tổ chức');expect(f.componentInstance.error()).toBe('');http.verify();f.destroy();
 });
 it('sends a staff navigation context without requesting a privileged role',()=>{
  const http=setup('checkin'),f=TestBed.createComponent(RegisterPageComponent);f.componentInstance.form.setValue({email:user.email,displayName:user.displayName,password:'Staff-password-123',confirmPassword:'Staff-password-123'});f.componentInstance.submit();const r=http.expectOne('/api/v1/auth/register');expect(r.request.body.context).toEqual({portal:'checkin',returnUrl:'/home'});expect(r.request.body.role).toBeUndefined();r.flush(null,{status:202,statusText:'Accepted'});http.verify();f.destroy();
 });
});
