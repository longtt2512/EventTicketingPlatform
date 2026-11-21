import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { SessionStore } from './index';
import { safeReturnUrl } from './return-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  if (!request.url.startsWith('/api/v1/') || request.url.includes('/auth/')) return next(request);
  const token = session.accessToken();
  const authenticated = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authenticated).pipe(catchError((error: HttpErrorResponse) => {
    if (error.status !== 401 || !token || error.error?.code === 'INVALID_CREDENTIALS') return throwError(() => error);
    return session.refresh().pipe(switchMap(refreshed => {
      if (!refreshed) {
        void router.navigate(['/login'], { queryParams: { expired: '1', returnUrl: safeReturnUrl(router.url) } });
        return throwError(() => error);
      }
      return next(request.clone({ setHeaders: { Authorization: `Bearer ${refreshed.accessToken}` } }));
    }));
  }));
};
