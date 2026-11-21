import { InjectionToken } from '@angular/core';
import { Observable, defer, firstValueFrom, from } from 'rxjs';

export interface RefreshCoordinator { run<T>(request: () => Observable<T>): Observable<T>; }
/** A cookie is shared across portal tabs; rotate it once at a time across the origin. */
export const REFRESH_COORDINATOR = new InjectionToken<RefreshCoordinator>('session refresh coordinator', {
  factory: () => ({
    run<T>(request: () => Observable<T>): Observable<T> {
      if (typeof navigator === 'undefined' || !navigator.locks) return defer(request);
      return defer(() => from(navigator.locks.request('etp-session-refresh', () => firstValueFrom(request()))));
    },
  }),
});
