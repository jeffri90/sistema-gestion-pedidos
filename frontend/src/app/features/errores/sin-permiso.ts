import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-sin-permiso',
  imports: [RouterLink],
  template: `
    <section class="tarjeta centrado">
      <span class="codigo-grande">403</span>
      <h1>No tiene permiso para esta pantalla</h1>
      <p class="texto-suave">Su rol <strong>{{ auth.rolPrincipal() }}</strong> no puede realizar esta operación.</p>
      <a class="btn btn-primario" routerLink="/">Ir al inicio</a>
    </section>
  `,
})
export class SinPermiso {
  protected readonly auth = inject(AuthService);
}
