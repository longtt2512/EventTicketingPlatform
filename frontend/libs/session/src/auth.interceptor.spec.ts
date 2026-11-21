import { REFRESH_COORDINATOR, RefreshCoordinator } from '@event-ticketing/session';
const directRefresh: RefreshCoordinator = { run: request => request() };
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { SessionStore } from './index';
import { User } from '@event-ticketing/models';

describe('Authenticated requests', () => {
  let http: HttpTestingController; let client: HttpClient; let session: SessionStore;
  const user: User = { id: 'fixture', email: 'fixture@example.test', displayName: 'Fixture', roles: ['CUSTOMER'], status: 'ACTIVE', emailVerified: true, preferredLocale: 'vi', createdAt: '' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [{ provide: REFRESH_COORDINATOR, useValue: directRefresh },provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController); client = TestBed.inject(HttpClient); session = TestBed.inject(SessionStore);
    session.setSession('old-token', user);
  });
  afterEach(() => http.verify());

  it('never sends the access token to third parties or public authentication endpoints', () => {
    for (const url of ['https://external.example/upload', '/api/v1/auth/login']) {
      client.get(url).subscribe(); const request = http.expectOne(url); expect(request.request.headers.has('Authorization')).toBeFalse(); request.flush({});
    }
  });
  it('rotates once and retries with the replacement token after access expiry', () => {
    let result: unknown; client.get('/api/v1/users/me').subscribe(value => result = value);
    const original = http.expectOne('/api/v1/users/me'); expect(original.request.headers.get('Authorization')).toBe('Bearer old-token');
    original.flush({ code: 'AUTH_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'new-token', user, expiresInSeconds: 600 });
    const retry = http.expectOne('/api/v1/users/me'); expect(retry.request.headers.get('Authorization')).toBe('Bearer new-token'); retry.flush(user);
    expect(result).toEqual(user);
  });
  it('does not retry a wrong current password or a forbidden operation', () => {
    for (const [code, status] of [['INVALID_CREDENTIALS', 401], ['FORBIDDEN', 403]] as const) {
      let failed = false; client.patch('/api/v1/users/me/password', {}).subscribe({ error: () => failed = true });
      http.expectOne('/api/v1/users/me/password').flush({ code }, { status, statusText: 'Denied' });
      http.expectNone('/api/v1/auth/refresh'); expect(failed).toBeTrue(); expect(session.isAuthenticated()).toBeTrue();
    }
  });
  it('clears revoked sessions and redirects to login without retrying the protected mutation', () => {
    const navigation = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    client.patch('/api/v1/users/me', { displayName: 'New' }).subscribe({ error: () => undefined });
    http.expectOne('/api/v1/users/me').flush({ code: 'AUTH_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/v1/auth/refresh').flush({ code: 'INVALID_REFRESH_TOKEN' }, { status: 401, statusText: 'Unauthorized' });
    http.expectNone('/api/v1/users/me'); expect(session.isAuthenticated()).toBeFalse(); expect(navigation).toHaveBeenCalledWith(['/login'], { queryParams: { expired: '1', returnUrl: '/home' } });
  });
  it('passes an unauthenticated rejection through without a refresh loop', () => {
    session.clear(); let failed = false; client.get('/api/v1/users/me').subscribe({ error: () => failed = true });
    http.expectOne('/api/v1/users/me').flush({}, { status: 401, statusText: 'Unauthorized' }); http.expectNone('/api/v1/auth/refresh'); expect(failed).toBeTrue();
  });
});
