import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <a routerLink="/" class="marca">Gestión de Pedidos</a>
      <nav>
        <a routerLink="/" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Inicio</a>
        <a routerLink="/productos" routerLinkActive="activo">Productos</a>
        <a routerLink="/clientes" routerLinkActive="activo">Clientes</a>
        <a routerLink="/pedidos" routerLinkActive="activo">Pedidos</a>
        <a routerLink="/diagnostico" routerLinkActive="activo">Errores HTTP</a>
      </nav>
      <div class="usuario">
        <span>{{ auth.username() }} <span class="rol">{{ auth.rolPrincipal() }}</span></span>
        <button type="button" class="btn btn-sm btn-claro" (click)="auth.logout()">Cerrar sesión</button>
      </div>
    </header>
  `,
})
export class Navbar {
  protected readonly auth = inject(AuthService);
}
