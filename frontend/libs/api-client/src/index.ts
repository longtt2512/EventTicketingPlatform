import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AuthContext,
  ForgotPasswordRequest,
  LoginRequest,
  OrganizerApplication,
  Organization,
  RegisterRequest,
  RejectApplicationRequest,
  SessionResponse,
  User,
} from './models';

export { AuthContext, ForgotPasswordRequest, LoginRequest, RegisterRequest, RejectApplicationRequest } from './models';

export const API_BASE_URL = '/api/v1';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/register`, request);
  }

  resendVerification(email: string, context?: AuthContext): Observable<{ retryAfterSeconds: number }> {
    return this.http.post<{ retryAfterSeconds: number }>(`${API_BASE_URL}/auth/resend-verification`, { email, context });
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/verify-email`, { token });
  }

  login(request: LoginRequest): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(`${API_BASE_URL}/auth/login`, request, { withCredentials: true });
  }

  refresh(): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(`${API_BASE_URL}/auth/refresh`, {}, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/logout`, {}, { withCredentials: true });
  }

  me(): Observable<User> {
    return this.http.get<User>(`${API_BASE_URL}/users/me`);
  }

  updateProfile(request: { displayName: string; preferredLocale: string }): Observable<User> {
    return this.http.patch<User>(`${API_BASE_URL}/users/me`, request);
  }

  forgotPassword(email: string, context?: AuthContext): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/forgot-password`, { email, context });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/reset-password`, { token, newPassword });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${API_BASE_URL}/users/me/password`, { currentPassword, newPassword });
  }
}

@Injectable({ providedIn: 'root' })
export class OrganizationApiService {
  private readonly http = inject(HttpClient);

  submitApplication(request: OrganizerApplicationRequest): Observable<OrganizerApplication> {
    return this.http.post<OrganizerApplication>(`${API_BASE_URL}/organizer-applications`, request);
  }

  myApplication(): Observable<OrganizerApplication> {
    return this.http.get<OrganizerApplication>(`${API_BASE_URL}/organizer-applications/me`);
  }

  listApplications(): Observable<OrganizerApplication[]> {
    return this.http.get<OrganizerApplication[]>(`${API_BASE_URL}/admin/organizer-applications`);
  }

  approveApplication(id: string): Observable<Organization> {
    return this.http.post<Organization>(`${API_BASE_URL}/admin/organizer-applications/${id}/approve`, {});
  }

  rejectApplication(id: string, request: RejectApplicationRequest): Observable<OrganizerApplication> {
    return this.http.post<OrganizerApplication>(`${API_BASE_URL}/admin/organizer-applications/${id}/reject`, request);
  }
}

export interface OrganizerApplicationRequest {
  organizationName: string;
  contactEmail: string;
  description?: string;
  verificationData?: string;
}
