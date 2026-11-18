import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ErrorUi, aplicarErroresServidor, describirError } from '../../core/http/http-error';
import { ProductoRequest } from '../../core/models/api.models';
import { NotificacionService } from '../../core/notificacion.service';
import { ProductoService } from '../../core/services/producto.service';
import { CampoError } from '../../shared/campo-error/campo-error';
import { ErrorApi } from '../../shared/error-api/error-api';
import { PATRONES, entero, limpiar, maxDecimales } from '../../shared/validadores';

@Component({
  selector: 'app-producto-form',
  imports: [ReactiveFormsModule, RouterLink, CampoError, ErrorApi],
  template: `
    <div class="encabezado">
      <h1>{{ id() ? 'Editar producto' : 'Nuevo producto' }}</h1>
      <a class="btn btn-claro" routerLink="/productos">Volver</a>
    </div>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else if (!noExiste()) {
      <form class="tarjeta formulario" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="grilla-2">
          <div>
            <label for="sku">SKU</label>
            <input id="sku" formControlName="sku" placeholder="ALI-010" />
            <app-campo-error [control]="form.controls.sku" patron="SKU: 2 a 30 letras, números o guiones" />
          </div>
          <div>
            <label for="categoria">Categoría</label>
            <input id="categoria" formControlName="categoria" placeholder="Alimentos" />
            <app-campo-error [control]="form.controls.categoria" />
          </div>
        </div>
        <label for="nombre">Nombre</label>
        <input id="nombre" formControlName="nombre" placeholder="Aceite vegetal 1 L" />
        <app-campo-error [control]="form.controls.nombre" />
        <div class="grilla-2">
          <div>
            <label for="precio">Precio (S/)</label>
            <input id="precio" type="number" step="0.01" min="0.01" formControlName="precio" />
            <app-campo-error [control]="form.controls.precio" />
          </div>
          <div>
            <label for="stock">Stock</label>
            <input id="stock" type="number" step="1" min="0" formControlName="stock" />
            <app-campo-error [control]="form.controls.stock" />
          </div>
        </div>
        <div class="botones">
          <button class="btn btn-primario" type="submit" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : 'Guardar' }}
          </button>
          <a class="btn btn-claro" routerLink="/productos">Cancelar</a>
        </div>
      </form>
    }
  `,
})
export class ProductoForm implements OnInit {
  /** Parámetro :id de la ruta; vacío al crear. */
  readonly id = input<string>();

  private readonly service = inject(ProductoService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly noExiste = signal(false);
  protected readonly error = signal<ErrorUi | null>(null);

  protected readonly form = inject(FormBuilder).group({
    sku: ['', [Validators.required, Validators.pattern(PATRONES.sku)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    categoria: ['', [Validators.required, Validators.maxLength(60)]],
    precio: [null as number | null, [Validators.required, Validators.min(0.01), Validators.max(9999999999.99), maxDecimales(2)]],
    stock: [null as number | null, [Validators.required, Validators.min(0), Validators.max(1000000), entero]],
  });

  ngOnInit(): void {
    const id = Number(this.id());
    if (!id) return;
    this.cargando.set(true);
    this.service.obtener(id).subscribe({
      next: (p) => {
        this.form.patchValue(p);
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
    const datos = limpiar(this.form.getRawValue()) as ProductoRequest;
    const id = Number(this.id());
    const op = id ? this.service.actualizar(id, datos) : this.service.crear(datos);
    op.subscribe({
      next: (p) => {
        this.avisos.exito(id ? `Producto ${p.sku} actualizado` : `Producto ${p.sku} creado`);
        this.router.navigate(['/productos']);
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
