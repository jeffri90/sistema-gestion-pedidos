import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/api.models';
import { AuthService } from './auth.service';

/** Exige sesión iniciada; si no, envía al login recordando la ruta pedida. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.autenticado()) return true;
  return inject(Router).createUrlTree(['/login'], { queryParams: { volver: state.url } });
};

/** Exige uno de los roles indicados en route.data['roles']. */
export const rolGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { volver: state.url } });
  }
  const roles = (route.data['roles'] ?? []) as Rol[];
  return roles.length === 0 || auth.tieneRol(...roles)
    ? true
    : router.createUrlTree(['/sin-permiso']);
};

/** Evita mostrar el login a quien ya inició sesión. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).createUrlTree(['/']) : true;
};
