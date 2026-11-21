import { Component } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { LoginPageComponent } from '../../auth/src/login-page.component';
import { RegisterPageComponent } from '../../auth/src/register-page.component';
import { OrganizationDetailComponent } from '../../organization/src/organization-detail.component';
import { UsersPageComponent } from '../../admin/src/users-page.component';
import { ConfirmDialogComponent, ConfirmationState } from './confirm-dialog.component';
import { PendingEdits, unsavedChangesGuard } from './unsaved-changes';
import { safeReturnUrl } from '@event-ticketing/session';

@Component({
    imports: [ConfirmDialogComponent], template: `<button id="trigger" (click)="open()">Khóa người dùng</button>@if(decision.current();as value){<etp-confirm-dialog [confirmation]="value" (resolved)="decision.resolve($event)"/>}`
})
class DialogHost {
 readonly decision=new ConfirmationState(); executed=0;
 open(){this.decision.ask({title:'Khóa tài khoản?',subject:'Nguyễn Thử',description:'Người dùng sẽ mất quyền truy cập.',confirmLabel:'Khóa tài khoản',danger:true},()=>this.executed++);}
}

describe('P1 form feedback and safe navigation',()=>{
 let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('explains an invalid submit, focuses the first field, and clears feedback after correction',fakeAsync(()=>{
   const f=TestBed.createComponent(LoginPageComponent);f.changeDetectorRef.markForCheck(); f.detectChanges();
   const email=f.nativeElement.querySelector('#email') as HTMLInputElement;
   expect(f.nativeElement.querySelector('button').disabled).toBeFalse();
   f.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));f.changeDetectorRef.markForCheck(); f.detectChanges();tick(20);
   expect(document.activeElement).toBe(email);expect(email.getAttribute('aria-invalid')).toBe('true');
   expect(f.nativeElement.querySelector('#email-error').textContent).toContain('Vui lòng nhập');http.expectNone('/api/v1/auth/login');
   email.value='wrong';email.dispatchEvent(new Event('input'));f.changeDetectorRef.markForCheck(); f.detectChanges();expect(f.nativeElement.querySelector('#email-error').textContent).toContain('ten@example.com');
   email.value='valid@example.test';email.dispatchEvent(new Event('input'));f.changeDetectorRef.markForCheck(); f.detectChanges();expect(email.getAttribute('aria-invalid')).toBe('false');expect(f.nativeElement.querySelector('#email-error').hidden).toBeTrue();
 }));
 it('keeps password help and exposes mismatches beside the confirmation input',()=>{
   const f=TestBed.createComponent(RegisterPageComponent);f.changeDetectorRef.markForCheck(); f.detectChanges();
   f.componentInstance.form.setValue({displayName:'Name',email:'user@example.test',password:'short',confirmPassword:'different'});f.componentInstance.form.markAllAsTouched();f.changeDetectorRef.markForCheck(); f.detectChanges();
   f.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
   expect(f.nativeElement.querySelector('#password').getAttribute('aria-describedby')).toBe('password-help password-error');
   expect(f.nativeElement.querySelector('#confirmPassword-error').textContent).toContain('chưa khớp');
   f.componentInstance.form.patchValue({password:'long-enough-123',confirmPassword:'long-enough-123'});f.changeDetectorRef.markForCheck(); f.detectChanges();expect(f.componentInstance.form.valid).toBeTrue();
   f.componentInstance.form.controls.password.setValue('changed-again-123');f.changeDetectorRef.markForCheck(); f.detectChanges();expect(f.componentInstance.form.valid).toBeFalse();
 });
 it('blocks leaving a dirty form when cancelled and allows accepted or saved navigation',()=>{
   const confirm=spyOn(window,'confirm').and.returnValue(false);const component={hasUnsavedChanges:()=>true};
   const guard=()=>TestBed.runInInjectionContext(()=>unsavedChangesGuard(component,{} as never,{} as never,{} as never));
   expect(guard()).toBeFalse();confirm.and.returnValue(true);expect(guard()).toBeTrue();component.hasUnsavedChanges=()=>false;confirm.calls.reset();expect(guard()).toBeTrue();expect(confirm).not.toHaveBeenCalled();
 });
 it('does not keep a discard approval after the logout navigation',()=>{
   const edits=TestBed.inject(PendingEdits);edits.allowNextNavigation();expect(edits.consumeApproval()).toBeTrue();expect(edits.consumeApproval()).toBeFalse();
 });
 it('preserves only internal destinations, including organization section context',()=>{
   expect(safeReturnUrl('/organizations/abc?section=payouts')).toBe('/organizations/abc?section=payouts');
   for(const value of ['https://example.test','//example.test','/\\example.test','/login','/reset-password#token=secret','/profile\n'])expect(safeReturnUrl(value)).toBe('/home');
 });
 it('restores focus after cancelling a confirmation and invokes an action only once',()=>{
   const f=TestBed.createComponent(DialogHost);f.changeDetectorRef.markForCheck(); f.detectChanges();const trigger=f.nativeElement.querySelector('#trigger') as HTMLButtonElement;trigger.focus();trigger.click();f.changeDetectorRef.markForCheck(); f.detectChanges();
   const dialog=f.nativeElement.querySelector('dialog') as HTMLDialogElement;
   expect(dialog.open).toBeTrue();expect((document.activeElement as HTMLElement).textContent).toBe('Hủy');
   const controls=dialog.querySelectorAll('button');
   controls[1].focus();const next=new KeyboardEvent('keydown',{key:'Tab',bubbles:true,cancelable:true});controls[1].dispatchEvent(next);
   expect(next.defaultPrevented).toBeTrue();expect(document.activeElement).toBe(controls[0]);
   const previous=new KeyboardEvent('keydown',{key:'Tab',shiftKey:true,bubbles:true,cancelable:true});controls[0].dispatchEvent(previous);
   expect(previous.defaultPrevented).toBeTrue();expect(document.activeElement).toBe(controls[1]);
   dialog.dispatchEvent(new Event('cancel',{cancelable:true}));f.changeDetectorRef.markForCheck(); f.detectChanges();expect(f.componentInstance.executed).toBe(0);expect(document.activeElement).toBe(trigger);
   trigger.click();f.changeDetectorRef.markForCheck(); f.detectChanges();f.componentInstance.decision.resolve(true);f.componentInstance.decision.resolve(true);expect(f.componentInstance.executed).toBe(1);
 });
 it('requires Admin confirmation, preserves a cancelled draft, and submits the reviewed reason',()=>{
   const f=TestBed.createComponent(UsersPageComponent);const c=f.componentInstance;
   const user={id:'target',displayName:'Nguyễn Thử',email:'test@example.test',status:'ACTIVE' as const,roles:['CUSTOMER'],emailVerified:true,preferredLocale:'vi',createdAt:''};
   http.expectOne('/api/v1/admin/users').flush({items:[user],nextCursor:null});f.changeDetectorRef.markForCheck(); f.detectChanges();c.select(user);c.form.controls.reason.setValue('Lý do đã xem');c.change();f.changeDetectorRef.markForCheck(); f.detectChanges();
   expect(f.nativeElement.querySelector('dialog').textContent).toContain(user.email);http.expectNone('/api/v1/admin/users/target/lock');c.confirmation.resolve(false);expect(c.form.controls.reason.value).toBe('Lý do đã xem');
   c.change();c.form.controls.reason.setValue('Changed after review');c.confirmation.resolve(true);
   const request=http.expectOne('/api/v1/admin/users/target/lock');expect(request.request.body.reason).toBe('Lý do đã xem');request.flush(null);http.expectOne('/api/v1/admin/users').flush({items:[],nextCursor:null});expect(c.selected()).toBeNull();
 });
});

describe('P1 organization section state',()=>{
 it('retains payout drafts in memory while switching sections and includes them in departure protection',async()=>{
   const query=new BehaviorSubject(convertToParamMap({section:'payouts'}));
   TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({id:'org'})},queryParamMap:query}}]});
   const http=TestBed.inject(HttpTestingController),router=TestBed.inject(Router);spyOn(router,'navigate').and.resolveTo(true);
   const f=TestBed.createComponent(OrganizationDetailComponent);
   http.expectOne('/api/v1/organizations/org').flush({id:'org',name:'Org',status:'ACTIVE',contactEmail:'org@example.test',address:'Test',description:'',version:0,owner:true,permission:'MANAGEMENT'});f.changeDetectorRef.markForCheck(); f.detectChanges();http.expectOne('/api/v1/organizations/org/payout-destinations').flush([]);f.changeDetectorRef.markForCheck(); f.detectChanges();await f.whenStable();
   const input=f.nativeElement.querySelector('#account-number') as HTMLInputElement;input.value='123456789';input.dispatchEvent(new Event('input'));f.changeDetectorRef.markForCheck(); f.detectChanges();await f.whenStable();
   expect(f.componentInstance.hasUnsavedChanges()).toBeTrue();query.next(convertToParamMap({section:'general'}));f.changeDetectorRef.markForCheck(); f.detectChanges();query.next(convertToParamMap({section:'payouts'}));f.changeDetectorRef.markForCheck(); f.detectChanges();
   expect((f.nativeElement.querySelector('#account-number') as HTMLInputElement).value).toBe('123456789');f.componentInstance.select('general');expect(router.navigate).toHaveBeenCalledWith([],jasmine.objectContaining({queryParams:{section:'general'},queryParamsHandling:'merge'}));
   expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);http.verify();
 });
});
