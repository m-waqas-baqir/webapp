import { UserRole } from '../../core/models/user-role';

/** URL / config keys (lowercase) */
export type DemoRoleKey = 'director' | 'admin' | 'agent';

export function userRoleToDemoKey(role: UserRole | undefined | null): DemoRoleKey {
  switch (role) {
    case UserRole.DIRECTOR:
      return 'director';
    case UserRole.ADMIN:
      return 'admin';
    default:
      return 'agent';
  }
}

export function parseDemoRoleParam(raw: string | null | undefined): DemoRoleKey | null {
  if (!raw) {
    return null;
  }
  const v = raw.trim().toLowerCase();
  if (v === 'director' || v === 'admin' || v === 'agent') {
    return v;
  }
  return null;
}
