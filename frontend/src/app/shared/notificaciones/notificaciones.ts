import { Component, inject } from '@angular/core';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-notificaciones',
  template: `
    <div class="toasts" aria-live="polite">
      @for (n of avisos.lista(); track n.id) {
        <div class="toast toast-{{ n.tipo }}" role="status">
          <span>{{ n.texto }}</span>
          <button type="button" class="toast-cerrar" (click)="avisos.cerrar(n.id)" aria-label="Cerrar">×</button>
        </div>
      }
    </div>
  `,
})
export class Notificaciones {
  protected readonly avisos = inject(NotificacionService);
}
