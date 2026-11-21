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

export interface ProblemDetails {
  code?: string;
  detail?: string;
  status?: number;
  requestId?: string;
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
