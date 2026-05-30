import { UserRole } from './user-role';

/** Matches backend {@code AuthResponse}. */
export interface AuthResponseDto {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: number;
  email: string;
  name: string;
  role: UserRole;
  /** Present on current API; treated as true when missing. */
  active?: boolean;
  /** Fine-grained codes; empty when missing. */
  permissions?: string[];
}
