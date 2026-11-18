import { Component, input } from '@angular/core';
import { EstadoPedido } from '../../core/models/api.models';

@Component({
  selector: 'app-estado-badge',
  template: `<span class="badge badge-{{ estado().toLowerCase() }}">{{ estado() }}</span>`,
})
export class EstadoBadge {
  readonly estado = input.required<EstadoPedido>();
}
