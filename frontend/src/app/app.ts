import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { Navbar } from './shared/navbar/navbar';
import { Notificaciones } from './shared/notificaciones/notificaciones';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, Notificaciones],
  template: `
    @if (auth.username()) {
      <app-navbar />
    }
    <main class="contenedor">
      <router-outlet />
    </main>
    <app-notificaciones />
  `,
})
export class App {
  protected readonly auth = inject(AuthService);
}
