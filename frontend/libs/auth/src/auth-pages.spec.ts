import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { LoginPageComponent } from './login-page.component';
import { RegisterPageComponent } from './register-page.component';
import { RecoveryPageComponent } from './recovery-page.component';
import { ProfilePageComponent } from './profile-page.component';
import { HomePageComponent } from './home-page.component';
import { errorMessage } from './feedback';
import { SessionStore } from '@event-ticketing/session';
import { AppShellComponent, WORKSPACE } from '@event-ticketing/ui';
import { User } from '@event-ticketing/models';

describe('Account workflows', () => {
  let http: HttpTestingController; let session: SessionStore; let router: Router;
  const user: User = { id: 'fixture', email: 'fixture@example.test', displayName: 'Nguyễn Thử', roles: ['CUSTOMER'], status: 'ACTIVE', emailVerified: true, preferredLocale: 'vi', createdAt: '' };
  const response = { accessToken: 'memory-fixture', user, expiresInSeconds: 600 };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController); session = TestBed.inject(SessionStore); router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true); spyOn(router, 'navigate').and.resolveTo(true);
  });
  afterEach(() => http.verify());
  function login() { const fixture = TestBed.createComponent(LoginPageComponent); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); fixture.componentInstance.form.setValue({ email: user.email, password: 'Valid-password-123!' }); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); return fixture; }
  function register() { const fixture = TestBed.createComponent(RegisterPageComponent); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); fixture.componentInstance.form.setValue({ displayName: user.displayName, email: user.email, password: 'Valid-password-123!', confirmPassword: 'Valid-password-123!' }); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); return fixture; }
  function profile() { session.setSession('old-token', user); const fixture = TestBed.createComponent(ProfilePageComponent); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); return fixture; }
  function fillPassword(fixture: ComponentFixture<ProfilePageComponent>) { fixture.componentInstance.password.setValue({ currentPassword: 'Old-password-123!', newPassword: 'New-password-123!', confirmPassword: 'New-password-123!' }); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); }

  it('signs in, stores only the access session and opens the workspace', () => {
    const fixture = login(); fixture.componentInstance.submit();
    const request = http.expectOne('/api/v1/auth/login'); expect(request.request.withCredentials).toBeTrue(); request.flush(response);
    expect(session.user()?.id).toBe(user.id); expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
  });
  it('allows retry after a failed login instead of leaving the button busy', () => {
    const fixture = login(); fixture.componentInstance.submit();
    http.expectOne('/api/v1/auth/login').flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });
    fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Email hoặc mật khẩu');
    expect(fixture.componentInstance.loading()).toBeFalse(); fixture.componentInstance.submit(); http.expectOne('/api/v1/auth/login').flush(response);
  });
  it('does not send invalid or duplicate in-flight login submissions', () => {
    const fixture = login(); fixture.componentInstance.form.controls.email.setValue('invalid'); fixture.componentInstance.submit(); http.expectNone('/api/v1/auth/login');
    fixture.componentInstance.form.controls.email.setValue(user.email); fixture.componentInstance.submit(); fixture.componentInstance.submit(); http.expectOne('/api/v1/auth/login').flush(response);
  });
  it('requires matching registration passwords before sending a request', () => {
    const fixture = register(); fixture.componentInstance.form.controls.confirmPassword.setValue('Mismatch-123!'); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); fixture.componentInstance.form.markAllAsTouched(); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); fixture.componentInstance.submit();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true})); expect(fixture.nativeElement.textContent).toContain('chưa khớp'); http.expectNone('/api/v1/auth/register');
  });
  it('shows the generic email confirmation and clears passwords after registration', () => {
    const fixture = register(); fixture.componentInstance.submit(); http.expectOne('/api/v1/auth/register').flush(null, { status: 202, statusText: 'Accepted' }); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges();
    expect(fixture.componentInstance.sent()).toBeTrue(); expect(fixture.nativeElement.textContent).toContain('Kiểm tra hộp thư'); expect(fixture.componentInstance.form.controls.password.value).toBe('');
  });
  it('preserves registration data and releases loading when the network fails', () => {
    const fixture = register(); fixture.componentInstance.submit(); http.expectOne('/api/v1/auth/register').error(new ProgressEvent('network'));
    expect(fixture.componentInstance.loading()).toBeFalse(); expect(fixture.componentInstance.form.controls.email.value).toBe(user.email); expect(fixture.componentInstance.error()).toContain('kết nối');
  });
  it('blocks invalid and in-flight registration resubmission', () => {
    const fixture = register(); fixture.componentInstance.form.controls.displayName.setValue(''); fixture.componentInstance.submit(); http.expectNone('/api/v1/auth/register');
    fixture.componentInstance.form.controls.displayName.setValue(user.displayName); fixture.componentInstance.submit(); fixture.componentInstance.submit(); http.expectOne('/api/v1/auth/register').flush(null);
  });
  it('updates profile and the currently displayed user', () => {
    const fixture = profile(); fixture.componentInstance.profile.setValue({ displayName: 'New name', preferredLocale: 'en' }); fixture.componentInstance.save();
    http.expectOne('/api/v1/users/me').flush({ ...user, displayName: 'New name', preferredLocale: 'en' });
    expect(session.user()?.displayName).toBe('New name'); expect(fixture.componentInstance.saved()).toBeTrue();
  });
  it('preserves profile edits after a server error and prevents duplicate saves', () => {
    const fixture = profile(); fixture.componentInstance.save(); fixture.componentInstance.save(); http.expectOne('/api/v1/users/me').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(fixture.componentInstance.saving()).toBeFalse(); expect(fixture.componentInstance.profileError()).toContain('thử lại');
    fixture.componentInstance.profile.controls.displayName.setValue(''); fixture.componentInstance.save(); http.expectNone('/api/v1/users/me');
  });
  it('requires matching new passwords before changing a password', () => {
    const fixture = profile(); fillPassword(fixture); fixture.componentInstance.password.controls.confirmPassword.setValue('Mismatch-123!'); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); fixture.componentInstance.password.markAllAsTouched(); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); fixture.componentInstance.changePassword();
    fixture.nativeElement.querySelectorAll('form')[1].dispatchEvent(new Event('submit',{bubbles:true,cancelable:true})); expect(fixture.nativeElement.textContent).toContain('chưa khớp'); http.expectNone('/api/v1/users/me/password');
  });
  it('clears the session and asks for login after a password change', () => {
    const fixture = profile(); fillPassword(fixture); fixture.componentInstance.changePassword(); http.expectOne('/api/v1/users/me/password').flush(null);
    expect(session.isAuthenticated()).toBeFalse(); expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { passwordChanged: '1', returnUrl: '/profile' } });
  });
  it('shows a wrong-current-password error without signing the user out', () => {
    const fixture = profile(); fillPassword(fixture); fixture.componentInstance.changePassword(); fixture.componentInstance.changePassword();
    http.expectOne('/api/v1/users/me/password').flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });
    expect(session.isAuthenticated()).toBeTrue(); expect(fixture.componentInstance.changing()).toBeFalse();
    fixture.componentInstance.password.reset(); fixture.componentInstance.changePassword(); http.expectNone('/api/v1/users/me/password');
  });
  it('does not claim logout succeeded if the server cannot revoke the cookie', () => {
    session.setSession('fixture', user); const fixture = TestBed.createComponent(AppShellComponent); fixture.componentInstance.logout(); fixture.componentInstance.logout();
    http.expectOne('/api/v1/auth/logout').error(new ProgressEvent('network'));
    expect(session.isAuthenticated()).toBeTrue(); expect(fixture.componentInstance.logoutError()).toContain('thu hồi');
    fixture.componentInstance.logout(); http.expectOne('/api/v1/auth/logout').flush(null); expect(session.isAuthenticated()).toBeFalse();
  });
  it('skips navigation by focusing main without following the base-relative fragment', () => {
    session.setSession('fixture', user);
    const fixture = TestBed.createComponent(AppShellComponent);fixture.detectChanges();
    const event = new MouseEvent('click', {bubbles:true,cancelable:true});
    fixture.nativeElement.querySelector('.skip-link').dispatchEvent(event);
    expect(event.defaultPrevented).toBeTrue();
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('main'));
  });
  it('renders the home actions from the current account', () => {
    session.setSession('fixture', user); const fixture = TestBed.createComponent(HomePageComponent); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain(user.displayName);
    expect(fixture.nativeElement.querySelector('a[href="/profile"]')).not.toBeNull();
  });
  it('provides recoverable messages for unknown and non-HTTP failures', () => {
    expect(errorMessage(new Error('internal'))).toContain('thử lại');
    expect(errorMessage(new HttpErrorResponse({ status: 500, error: null }))).toContain('thử lại');
  });
});

describe('Email verification and password recovery', () => {
  let http: HttpTestingController;
  const fragment = new BehaviorSubject<string | null>(null);
  function setup(mode: string, token = '') {
    fragment.next(token ? 'token=' + token : null);
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { data: { mode }, queryParamMap: convertToParamMap({}) }, fragment, queryParamMap: new BehaviorSubject(convertToParamMap({})) } },
    ] });
    http = TestBed.inject(HttpTestingController); const fixture = TestBed.createComponent(RecoveryPageComponent); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); return fixture;
  }
  afterEach(() => http.verify());
  it('rejects an absent verification token without sending an API call', () => {
    const fixture = setup('verify'); fixture.componentInstance.verifyEmail(); expect(fixture.nativeElement.textContent).toContain('Nhập email đã đăng ký'); http.expectNone('/api/v1/auth/verify-email');
  });
  it('resends with generic feedback and respects the server cooldown before retry', fakeAsync(() => {
    const fixture=setup('verify'); const c=fixture.componentInstance;
    c.resendVerification(); http.expectNone('/api/v1/auth/resend-verification');
    c.forgot.controls.email.setValue('pending@example.test'); c.resendVerification(); c.resendVerification();
    http.expectOne('/api/v1/auth/resend-verification').flush({retryAfterSeconds:60}); fixture.detectChanges();
    expect(c.remaining()).toBe(60); expect(c.resendSent()).toBeTrue(); c.resendVerification(); http.expectNone('/api/v1/auth/resend-verification');
    tick(2000); expect(c.remaining()).toBe(58); tick(58000); expect(c.remaining()).toBe(0);
    c.resendVerification(); http.expectOne('/api/v1/auth/resend-verification').error(new ProgressEvent('network'));
    expect(c.loading()).toBeFalse(); expect(c.error()).toContain('kết nối'); expect(c.forgot.controls.email.value).toBe('pending@example.test'); fixture.destroy();
  }));
  it('opens resend recovery after an expired link while retaining the login destination',()=>{
    const fixture=setup('verify','expired-token');const c=fixture.componentInstance;c.verifyEmail();
    http.expectOne('/api/v1/auth/verify-email').flush({code:'INVALID_VERIFICATION_TOKEN'},{status:422,statusText:'Expired'});fixture.detectChanges();
    expect(c.token).toBe('');expect(fixture.nativeElement.textContent).toContain('Gửi lại email xác thực');expect(c.error()).toContain('hết hạn');
  });
  it('accepts a new email fragment on an already open verification page', () => {
    const fixture = setup('verify', 'old-token'); fragment.next('token=new-token'); fixture.componentInstance.verifyEmail();
    const request = http.expectOne('/api/v1/auth/verify-email'); expect(request.request.body.token).toBe('new-token'); request.flush(null);
    expect(fixture.componentInstance.done()).toBeTrue(); expect(fixture.componentInstance.token).toBe('');
  });
  it('shows a consumed or expired token error and permits retry', () => {
    const fixture = setup('verify', 'used-token'); fixture.componentInstance.verifyEmail();
    http.expectOne('/api/v1/auth/verify-email').flush({ code: 'INVALID_VERIFICATION_TOKEN' }, { status: 422, statusText: 'Invalid' });
    expect(fixture.componentInstance.loading()).toBeFalse(); expect(fixture.componentInstance.error()).toContain('đã dùng');
  });
  it('uses a generic password-recovery result without exposing account existence', () => {
    const fixture = setup('forgot'); fixture.componentInstance.forgot.controls.email.setValue('any@example.test'); fixture.componentInstance.requestReset();
    http.expectOne('/api/v1/auth/forgot-password').flush(null, { status: 202, statusText: 'Accepted' }); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=status]').textContent).toContain('Nếu email thuộc');
  });
  it('does not submit an invalid email or a duplicate recovery request', () => {
    const fixture = setup('forgot'); fixture.componentInstance.requestReset(); http.expectNone('/api/v1/auth/forgot-password');
    fixture.componentInstance.forgot.controls.email.setValue('any@example.test'); fixture.componentInstance.requestReset(); fixture.componentInstance.requestReset();
    http.expectOne('/api/v1/auth/forgot-password').error(new ProgressEvent('network')); expect(fixture.componentInstance.loading()).toBeFalse();
  });
  it('checks reset confirmation, preserves input after failure, then clears it after success', () => {
    const fixture = setup('reset', 'reset-token'); const component = fixture.componentInstance;
    component.reset.setValue({ newPassword: 'New-password-123!', confirmPassword: 'Mismatch-123!' }); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); component.reset.markAllAsTouched(); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); component.resetPassword(); fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true})); expect(fixture.nativeElement.textContent).toContain('chưa khớp');
    component.reset.controls.confirmPassword.setValue('New-password-123!'); fixture.changeDetectorRef.markForCheck(); fixture.detectChanges(); component.resetPassword(); component.resetPassword();
    http.expectOne('/api/v1/auth/reset-password').flush({ code: 'INVALID_RESET_TOKEN' }, { status: 422, statusText: 'Invalid' });
    expect(component.loading()).toBeFalse(); expect(component.reset.controls.newPassword.value).toBe('New-password-123!');
    fragment.next('token=replacement-token'); component.resetPassword(); http.expectOne('/api/v1/auth/reset-password').flush(null);
    expect(component.done()).toBeTrue(); expect(component.reset.controls.newPassword.value).toBe('');
  });
  it('does not reset without a token or valid fields', () => {
    const fixture = setup('reset'); fixture.componentInstance.resetPassword(); http.expectNone('/api/v1/auth/reset-password');
  });
});
