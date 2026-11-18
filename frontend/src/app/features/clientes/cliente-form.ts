import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ErrorUi, aplicarErroresServidor, describirError } from '../../core/http/http-error';
import { NotificacionService } from '../../core/notificacion.service';
import { ClienteService } from '../../core/services/cliente.service';
import { CampoError } from '../../shared/campo-error/campo-error';
import { ErrorApi } from '../../shared/error-api/error-api';
import { PATRONES, limpiar } from '../../shared/validadores';

@Component({
  selector: 'app-cliente-form',
  imports: [ReactiveFormsModule, RouterLink, CampoError, ErrorApi],
  template: `
    <div class="encabezado">
      <h1>{{ id() ? 'Editar cliente' : 'Nuevo cliente' }}</h1>
      <a class="btn btn-claro" routerLink="/clientes">Volver</a>
    </div>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else if (!noExiste()) {
      <form class="tarjeta formulario" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="grilla-2">
          <div>
            <label for="ruc">RUC</label>
            <input id="ruc" formControlName="ruc" inputmode="numeric" maxlength="11" placeholder="20123456789" />
            <app-campo-error [control]="form.controls.ruc" patron="El RUC debe tener 11 dígitos" />
          </div>
          <div>
            <label for="telefono">Teléfono</label>
            <input id="telefono" formControlName="telefono" placeholder="999 111 222" />
            <app-campo-error [control]="form.controls.telefono" patron="Teléfono inválido: 7 a 20 dígitos, espacios, +, ( ) o -" />
          </div>
        </div>
        <label for="razonSocial">Razón social</label>
        <input id="razonSocial" formControlName="razonSocial" />
        <app-campo-error [control]="form.controls.razonSocial" />

        <label for="email">Correo</label>
        <input id="email" type="email" formControlName="email" placeholder="compras@empresa.pe" />
        <app-campo-error [control]="form.controls.email" />

        <label for="direccion">Dirección</label>
        <input id="direccion" formControlName="direccion" />
        <app-campo-error [control]="form.controls.direccion" />

        <div class="botones">
          <button class="btn btn-primario" type="submit" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : 'Guardar' }}
          </button>
          <a class="btn btn-claro" routerLink="/clientes">Cancelar</a>
        </div>
      </form>
    }
  `,
})
export class ClienteForm implements OnInit {
  readonly id = input<string>();

  private readonly service = inject(ClienteService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly noExiste = signal(false);
  protected readonly error = signal<ErrorUi | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(PATRONES.ruc)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [Validators.required, Validators.pattern(PATRONES.telefono)]],
    direccion: ['', [Validators.required, Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    const id = Number(this.id());
    if (!id) return;
    this.cargando.set(true);
    this.service.obtener(id).subscribe({
      next: (c) => {
        this.form.patchValue(c);
        this.cargando.set(false);
      },
      error: (e) => {
        const err = describirError(e);
        this.error.set(err);
        this.noExiste.set(err.status === 404);
        this.cargando.set(false);
      },
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const datos = limpiar(this.form.getRawValue());
    const id = Number(this.id());
    const op = id ? this.service.actualizar(id, datos) : this.service.crear(datos);
    op.subscribe({
      next: (c) => {
        this.avisos.exito(id ? `Cliente ${c.razonSocial} actualizado` : `Cliente ${c.razonSocial} creado`);
        this.router.navigate(['/clientes']);
      },
      error: (e) => {
        const err = describirError(e);
        err.detalles = aplicarErroresServidor(this.form, err);
        this.error.set(err);
        this.guardando.set(false);
      },
    });
  }
}
