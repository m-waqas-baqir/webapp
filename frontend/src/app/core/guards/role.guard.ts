import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { UserRole } from '../models/user-role';
import { AuthService } from '../services/auth.service';

/** Requires {@link Route.data.roles} — one of the listed roles. */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = route.data['roles'] as UserRole[] | undefined;
  if (!roles?.length) {
    return true;
  }
  if (auth.hasAnyRole(...roles)) {
    return true;
  }
  if (auth.isAuthenticated()) {
    return router.createUrlTree(['/app/dashboard']);
  }
  return router.createUrlTree(['/login']);
};
