/** Mirrors backend {@code PermissionCodes} — UI hints only; authorization is enforced server-side. */
export const PERMISSION_CODES = {
  OWNER_VIEW: 'OWNER_VIEW',
  OWNER_EDIT: 'OWNER_EDIT',
  PLOT_DELETE: 'PLOT_DELETE',
  USER_MANAGE: 'USER_MANAGE',
  ASSIGN_AGENT: 'ASSIGN_AGENT',
  VIEW_ACTIVITY_LOGS: 'VIEW_ACTIVITY_LOGS',
} as const;

export type PermissionCode = (typeof PERMISSION_CODES)[keyof typeof PERMISSION_CODES];
