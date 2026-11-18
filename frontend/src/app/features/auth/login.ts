import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { NotificacionService } from '../../core/notificacion.service';
import { CampoError } from '../../shared/campo-error/campo-error';
import { ErrorApi } from '../../shared/error-api/error-api';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, CampoError, ErrorApi],
  template: `
    <section class="login">
      <div class="tarjeta login-tarjeta">
        <h1>Gestión de Pedidos</h1>
        <p class="texto-suave">Ingrese con su usuario para usar el sistema.</p>

        @if (sesion() === 'expirada') {
          <div class="alerta alerta-info">Su sesión terminó. Vuelva a iniciar sesión.</div>
        }
        <app-error-api [error]="error()" />

        <form [formGroup]="form" (ngSubmit)="ingresar()" novalidate>
          <label for="username">Usuario</label>
          <input id="username" formControlName="username" autocomplete="username" />
          <app-campo-error [control]="form.controls.username" />

          <label for="password">Contraseña</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" />
          <app-campo-error [control]="form.controls.password" />

          <button class="btn btn-primario btn-bloque" type="submit" [disabled]="enviando()">
            {{ enviando() ? 'Ingresando…' : 'Ingresar' }}
          </button>
        </form>
        <p class="texto-suave nota">
          Usuarios de demostración: <code>admin</code>, <code>ventas</code> y <code>consulta</code>
          (las contraseñas están en el README del backend).
        </p>
      </div>
    </section>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  /** Parámetros de la URL: ?volver=/ruta y ?sesion=expirada */
  readonly volver = input<string>();
  readonly sesion = input<string>();

  protected readonly enviando = signal(false);
  protected readonly error = signal<ErrorUi | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(50)]],
    password: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const datos = this.form.getRawValue();
    this.auth.login({ username: datos.username.trim(), password: datos.password }).subscribe({
      next: () => {
        this.avisos.exito(`Bienvenido, ${this.auth.username()}`);
        const destino = this.volver();
        this.router.navigateByUrl(destino && destino.startsWith('/') ? destino : '/');
      },
      error: (e) => {
        this.enviando.set(false);
        this.error.set(describirError(e));
        this.form.controls.password.reset();
      },
    });
  }
}
