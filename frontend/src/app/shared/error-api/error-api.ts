import { Component, input } from '@angular/core';
import { ErrorUi } from '../../core/http/http-error';

/** Muestra la respuesta de error del backend (400, 401, 403, 404, 409…). */
@Component({
  selector: 'app-error-api',
  template: `
    @if (error(); as e) {
      <div class="alerta alerta-error" role="alert" [attr.data-status]="e.status">
        <strong>{{ e.titulo }}</strong>
        <p>{{ e.mensaje }}</p>
        @if (e.detalles.length) {
          <ul>
            @for (d of e.detalles; track d) {
              <li>{{ d }}</li>
            }
          </ul>
        }
      </div>
    }
  `,
})
export class ErrorApi {
  readonly error = input<ErrorUi | null>(null);
}
