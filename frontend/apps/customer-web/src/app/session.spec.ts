import { REFRESH_COORDINATOR, RefreshCoordinator } from '@event-ticketing/session';
const directRefresh: RefreshCoordinator = { run: request => request() };
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { SessionStore } from '@event-ticketing/session';
import { authGuard, roleGuard } from '@event-ticketing/auth';
import { User } from '@event-ticketing/models';

describe('Session and workspace authorization', () => {
  let session: SessionStore; let http: HttpTestingController;
  const user: User = { id: 'user-1', email: 'fixture@example.test', displayName: 'Fixture', preferredLocale: 'vi', status: 'ACTIVE', emailVerified: true, roles: ['CUSTOMER'], createdAt: '' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [{ provide: REFRESH_COORDINATOR, useValue: directRefresh },provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    session = TestBed.inject(SessionStore); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('restores a session from the cookie while keeping credentials out of browser storage', async () => {
    const restoring = session.restore();
    const request = http.expectOne('/api/v1/auth/refresh');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({ accessToken: 'memory-only-fixture', user, expiresInSeconds: 600 });
    await restoring;
    expect(session.user()?.id).toBe(user.id); expect(session.isAuthenticated()).toBeTrue();
    expect(localStorage.getItem('accessToken')).toBeNull(); expect(sessionStorage.getItem('accessToken')).toBeNull();
  });
  it('shares one refresh request across concurrent callers', () => {
    let responses = 0;
    session.refresh().subscribe(() => responses++); session.refresh().subscribe(() => responses++);
    http.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'new-token', user, expiresInSeconds: 600 });
    expect(responses).toBe(2);
  });
  it('clears stale identity when the refresh session is rejected', () => {
    session.setSession('old-token', user); session.refresh().subscribe();
    http.expectOne('/api/v1/auth/refresh').flush({ code: 'AUTH_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    expect(session.user()).toBeNull(); expect(session.accessToken()).toBeNull(); expect(session.isAuthenticated()).toBeFalse();
  });
  it('allows a current customer but rejects the admin workspace', () => {
    session.setSession('fixture', user);
    const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')({} as never, {} as never));
    expect(TestBed.inject(Router).serializeUrl(result as import('@angular/router').UrlTree)).toBe('/forbidden');
    expect(TestBed.runInInjectionContext(() => authGuard({} as never, {} as never))).toBeTrue();
  });
  it('sends anonymous visitors to login', () => {
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(TestBed.inject(Router).serializeUrl(result as import('@angular/router').UrlTree)).toBe('/login?returnUrl=%2Fhome');
  });
});
