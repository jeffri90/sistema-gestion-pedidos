import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { SKIP_AUTH } from '../auth/auth.interceptor';
import { NotificacionService } from '../notificacion.service';

/**
 * Manejo global: un 401 en una petición protegida significa que el JWT venció o fue
 * rechazado, así que se cierra la sesión y se vuelve al login. Los demás códigos
 * (400, 403, 404, 409) los muestra cada pantalla con <app-error-api>.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const avisos = inject(NotificacionService);
  return next(req).pipe(
    catchError((err: unknown) => {
      const esLogin = req.url === '/api/auth/login';
      if (err instanceof HttpErrorResponse && err.status === 401 && !esLogin && !req.context.get(SKIP_AUTH)) {
        avisos.error('Su sesión venció o el token fue rechazado (401). Inicie sesión de nuevo.');
        auth.logout('expirada');
      }
      return throwError(() => err);
    }),
  );
};
