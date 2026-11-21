import { unsavedChangesGuard } from '@event-ticketing/ui';
import { Routes } from '@angular/router';
import { authGuard, roleGuard } from '@event-ticketing/auth';
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('@event-ticketing/auth').then(m => m.LoginPageComponent) },
  { path: 'register', loadComponent: () => import('@event-ticketing/auth').then(m => m.RegisterPageComponent), canDeactivate: [unsavedChangesGuard] },
  { path: 'forgot-password', loadComponent: () => import('@event-ticketing/auth').then(m => m.RecoveryPageComponent), canDeactivate: [unsavedChangesGuard], data: { mode: 'forgot' } },
  { path: 'reset-password', loadComponent: () => import('@event-ticketing/auth').then(m => m.RecoveryPageComponent), canDeactivate: [unsavedChangesGuard], data: { mode: 'reset' } },
  { path: 'verify-email', loadComponent: () => import('@event-ticketing/auth').then(m => m.RecoveryPageComponent), canDeactivate: [unsavedChangesGuard], data: { mode: 'verify' } },
  { path: 'profile', loadComponent: () => import('@event-ticketing/auth').then(m => m.ProfilePageComponent), canDeactivate: [unsavedChangesGuard], canActivate: [authGuard] },
  { path: 'home', loadComponent: () => import('@event-ticketing/events').then(m => m.HomeDiscoveryComponent) },
  { path: 'events', loadComponent: () => import('@event-ticketing/events').then(m => m.DiscoveryComponent) },
  { path: 'events/:id', loadComponent: () => import('@event-ticketing/events').then(m => m.PublicEventComponent) },
  { path: 'booking/:eventId', loadComponent: () => import('@event-ticketing/booking').then(m => m.BookingComponent) },
  { path: 'checkout/:orderId', loadComponent: () => import('@event-ticketing/booking').then(m => m.OrderComponent), canActivate: [authGuard], data: {mode:'checkout'} },
  { path: 'payment/checkout/:attemptId', loadComponent: () => import('@event-ticketing/booking').then(m => m.PaymentCheckoutComponent), canActivate: [authGuard] },
  { path: 'payment/mock/:attemptId', redirectTo: 'payment/checkout/:attemptId', pathMatch: 'full' },
  { path: 'payment/result/:orderId', loadComponent: () => import('@event-ticketing/booking').then(m => m.OrderComponent), canActivate: [authGuard], data: {mode:'result'} },
  { path: 'account/tickets', loadComponent: () => import('@event-ticketing/tickets').then(m => m.TicketsComponent), canActivate: [authGuard] },
  { path: 'account/tickets/:ticketId', loadComponent: () => import('@event-ticketing/tickets').then(m => m.TicketDetailComponent), canActivate: [authGuard] },
  { path: 'account/orders', loadComponent: () => import('@event-ticketing/booking').then(m => m.OrdersComponent), canActivate: [authGuard] },
  { path: 'account/orders/:orderId', loadComponent: () => import('@event-ticketing/booking').then(m => m.OrderComponent), canActivate: [authGuard], data: {mode:'detail'} },
  { path: 'apply', loadComponent: () => import('@event-ticketing/organization').then(m => m.OrganizerApplicationPageComponent), canDeactivate: [unsavedChangesGuard], canActivate: [authGuard] },
  { path: 'forbidden', loadComponent: () => import('@event-ticketing/ui').then(m => m.StatusPageComponent), data: { forbidden: true } },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', loadComponent: () => import('@event-ticketing/ui').then(m => m.StatusPageComponent) },
];
