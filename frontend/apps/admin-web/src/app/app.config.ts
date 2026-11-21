import { APP_INITIALIZER, ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { SessionStore, authInterceptor } from '@event-ticketing/session';
import { WORKSPACE } from '@event-ticketing/ui';
import { routes } from './app.routes';
export const appConfig: ApplicationConfig = { providers: [
  provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
  provideHttpClient(withInterceptors([authInterceptor])),
  { provide: WORKSPACE, useValue: 'admin' },
  { provide: APP_INITIALIZER, multi: true, deps: [SessionStore], useFactory: (session: SessionStore) => () => session.restore() },
] };
