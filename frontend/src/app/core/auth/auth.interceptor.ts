import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/** Permite que una petición envíe su propio encabezado Authorization (p. ej. token inválido). */
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);

/** Agrega "Authorization: Bearer <JWT>" a toda petición /api salvo el login. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/') || req.url === '/api/auth/login' || req.context.get(SKIP_AUTH)) {
    return next(req);
  }
  const token = inject(AuthService).token();
  return next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req);
};
