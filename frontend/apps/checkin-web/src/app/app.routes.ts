import { checkinPendingGuard } from '../../../../libs/tickets/src/checkin-guard';
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
  { path: 'home', loadComponent: () => import('@event-ticketing/auth').then(m => m.HomePageComponent), canActivate: [authGuard] },
  { path: 'assignments', loadComponent: () => import('@event-ticketing/organization').then(m => m.StaffEventsComponent), canActivate: [authGuard] },
  { path: 'check-in/events', loadComponent: () => import('@event-ticketing/tickets').then(m => m.CheckinEventsComponent), canActivate: [authGuard] },
  { path: 'check-in/events/:eventId/scan', loadComponent: () => import('@event-ticketing/tickets').then(m => m.CheckinWorkspaceComponent), canActivate: [authGuard], canDeactivate: [checkinPendingGuard], data:{mode:'scan'} },
  { path: 'check-in/events/:eventId/lookup', loadComponent: () => import('@event-ticketing/tickets').then(m => m.CheckinWorkspaceComponent), canActivate: [authGuard], canDeactivate: [checkinPendingGuard], data:{mode:'lookup'} },
  { path: 'check-in/events/:eventId/history', loadComponent: () => import('@event-ticketing/tickets').then(m => m.CheckinWorkspaceComponent), canActivate: [authGuard], canDeactivate: [checkinPendingGuard], data:{mode:'history'} },
  { path: 'check-in/events/:eventId/stats', loadComponent: () => import('@event-ticketing/tickets').then(m => m.CheckinWorkspaceComponent), canActivate: [authGuard], canDeactivate: [checkinPendingGuard], data:{mode:'stats'} },
  { path: 'forbidden', loadComponent: () => import('@event-ticketing/ui').then(m => m.StatusPageComponent), data: { forbidden: true } },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', loadComponent: () => import('@event-ticketing/ui').then(m => m.StatusPageComponent) },
];
