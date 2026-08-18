import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard = (roles: string[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  if (!roles.includes(auth.user()?.role)) {
    return router.createUrlTree(['/unauthorized']);
  }
  return true;
};
