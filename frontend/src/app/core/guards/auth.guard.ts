import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Requires a valid (non-expired) session; redirects to `/login` with `returnUrl`. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  auth.logout(false);
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
