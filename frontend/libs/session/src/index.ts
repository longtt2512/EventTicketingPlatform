import { REFRESH_COORDINATOR } from './refresh-lock';
import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { SessionResponse, User } from '@event-ticketing/models';
import { Observable, catchError, finalize, firstValueFrom, of, shareReplay, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly coordinator = inject(REFRESH_COORDINATOR);
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly accessTokenState = signal<string | null>(null);
  private readonly userState = signal<User | null>(null);
  private refreshing?: Observable<SessionResponse | null>;
  readonly accessToken = this.accessTokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => this.accessTokenState() !== null && this.userState() !== null);

  setSession(accessToken: string, user: User): void { this.accessTokenState.set(accessToken); this.userState.set(user); }
  updateUser(user: User): void { this.userState.set(user); }
  clear(): void { this.accessTokenState.set(null); this.userState.set(null); }
  async restore(): Promise<void> { await firstValueFrom(this.refresh()); }

  refresh(): Observable<SessionResponse | null> {
    if (!this.refreshing) {
      this.refreshing = this.coordinator.run(() => this.http.post<SessionResponse>('/api/v1/auth/refresh', {}, { withCredentials: true })).pipe(
        tap(session => this.setSession(session.accessToken, session.user)),
        catchError(() => { this.clear(); return of(null); }),
        finalize(() => { this.refreshing = undefined; }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.refreshing;
  }
}

export { authInterceptor } from './auth.interceptor';
export { safeReturnUrl } from './return-url';

export { REFRESH_COORDINATOR } from './refresh-lock';
export type { RefreshCoordinator } from './refresh-lock';
