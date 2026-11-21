import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore, safeReturnUrl } from '@event-ticketing/session';

export const authGuard: CanActivateFn = (_route, state) => inject(SessionStore).isAuthenticated() ||
  inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: safeReturnUrl(state.url) } });
export function roleGuard(role: string): CanActivateFn {
  return (_route, state) => {
    const session = inject(SessionStore); const router = inject(Router);
    if (!session.isAuthenticated()) return router.createUrlTree(['/login'], { queryParams: { returnUrl: safeReturnUrl(state.url) } });
    return session.user()?.roles.includes(role) || router.parseUrl('/forbidden');
  };
}
