import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-encontrada',
  imports: [RouterLink],
  template: `
    <section class="tarjeta centrado">
      <span class="codigo-grande">404</span>
      <h1>Página no encontrada</h1>
      <p class="texto-suave">La dirección no corresponde a ninguna pantalla del sistema.</p>
      <a class="btn btn-primario" routerLink="/">Ir al inicio</a>
    </section>
  `,
})
export class NoEncontrada {}
