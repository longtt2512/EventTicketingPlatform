export interface AuthContext { portal: "customer" | "organizer" | "admin" | "checkin"; returnUrl?: string; }

export interface RegisterRequest {
  context?: AuthContext;
  email: string;
  password: string;
  displayName: string;
  locale?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  context?: AuthContext;
  email: string;
}

export interface RejectApplicationRequest {
  reason: string;
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
  status: string;
  emailVerified: boolean;
  preferredLocale: string;
  createdAt: string;
}

export interface SessionResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface OrganizerApplication {
  id: string;
  applicantUserId: string;
  organizationName: string;
  contactEmail: string;
  description?: string;
  status: string;
  rejectionReason?: string;
  decidedByUserId?: string;
  decidedAt?: string;
  createdAt?: string;
}

export interface Organization {
  id: string;
  name: string;
  status: string;
  createdByUserId: string;
}
