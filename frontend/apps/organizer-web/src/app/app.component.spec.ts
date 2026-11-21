import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { WORKSPACE } from '@event-ticketing/ui';
import { SessionStore } from '@event-ticketing/session';

describe('Workspace shell', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), {provide:WORKSPACE,useValue:'organizer'}] }));
  it('shows sign-in navigation without private account details to a guest', () => {
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('a[href="/login"]')?.textContent).toContain('Đăng nhập');
    expect(element.querySelector('.portal-sidebar, .account-sidebar, .account-link')).toBeNull();
    expect(element.textContent).not.toContain('Fixture');
    expect(element.querySelector('a[href="#main-content"]')).not.toBeNull();
  });
  it('removes private navigation when the session is cleared', () => {
    const session = TestBed.inject(SessionStore);
    session.setSession('fixture', { id: 'id', email: 'fixture@example.test', displayName: 'Fixture', roles: ['CUSTOMER'], status: 'ACTIVE', emailVerified: true, preferredLocale: 'vi', createdAt: '' });
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.account-link')?.getAttribute('aria-label')).toBe('Fixture');
    session.clear(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.portal-sidebar, .account-sidebar, .account-link')).toBeNull();
    expect(fixture.nativeElement.querySelector('button[aria-label="Đăng xuất"]')).toBeNull();
  });
});
