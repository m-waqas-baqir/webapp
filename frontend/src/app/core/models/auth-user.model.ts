import { UserRole } from './user-role';

/** Cached session user (from login response or /api/v1/me). */
export interface AuthUser {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  active: boolean;
  permissions: string[];
}

