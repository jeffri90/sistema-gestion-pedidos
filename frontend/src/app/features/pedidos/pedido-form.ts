import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ErrorUi, aplicarErroresServidor, describirError } from '../../core/http/http-error';
import { Cliente, Pedido, PedidoRequest, Producto } from '../../core/models/api.models';
import { NotificacionService } from '../../core/notificacion.service';
import { ClienteService } from '../../core/services/cliente.service';
import { PedidoService } from '../../core/services/pedido.service';
import { ProductoService } from '../../core/services/producto.service';
import { CampoError } from '../../shared/campo-error/campo-error';
import { ErrorApi } from '../../shared/error-api/error-api';
import { entero } from '../../shared/validadores';

const MAX_LINEAS = 50;

/** Un producto no puede repetirse en el mismo pedido (regla del backend: 409). */
function sinRepetidos(arr: AbstractControl): ValidationErrors | null {
  const ids = (arr as FormArray).controls.map((g) => g.get('productoId')?.value).filter((v) => v != null);
  return new Set(ids).size !== ids.length ? { repetidos: true } : null;
}

@Component({
  selector: 'app-pedido-form',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, CampoError, ErrorApi],
  template: `
    <div class="encabezado">
      <h1>{{ id() ? 'Editar pedido #' + id() : 'Nuevo pedido' }}</h1>
      <a class="btn btn-claro" [routerLink]="id() ? ['/pedidos', id()] : ['/pedidos']">Volver</a>
    </div>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else if (bloqueado()) {
      <div class="alerta alerta-info">
        El pedido está {{ bloqueado() }}. Solo se pueden editar pedidos en BORRADOR.
      </div>
    } @else if (listo()) {
      <form class="tarjeta formulario" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <label for="cliente">Cliente</label>
        <select id="cliente" formControlName="clienteId">
          <option [ngValue]="null" disabled>Seleccione un cliente</option>
          @for (c of clientes(); track c.id) {
            <option [ngValue]="c.id">{{ c.razonSocial }} ({{ c.ruc }})</option>
          }
        </select>
        <app-campo-error [control]="form.controls.clienteId" />

        <h2 class="subtitulo">Productos</h2>
        <table class="tabla tabla-form" formArrayName="items">
          <thead>
            <tr><th>Producto</th><th class="col-cant">Cantidad</th><th class="num">Precio</th><th class="num">Subtotal</th><th></th></tr>
          </thead>
          <tbody>
            @for (linea of items.controls; track linea; let i = $index) {
              <tr [formGroupName]="i">
                <td>
                  <select formControlName="productoId" [attr.aria-label]="'Producto de la línea ' + (i + 1)">
                    <option [ngValue]="null" disabled>Seleccione…</option>
                    @for (p of productos(); track p.id) {
                      <option [ngValue]="p.id">{{ p.sku }} · {{ p.nombre }} (stock {{ p.stock }})</option>
                    }
                  </select>
                  <app-campo-error [control]="linea.get('productoId')" />
                  @if (repetido(i)) {
                    <small class="campo-error">Este producto ya está en otra línea</small>
                  }
                  @if (sinStock(i); as stock) {
                    <small class="campo-aviso">Solo hay {{ stock }} en stock: el pedido podrá guardarse, pero no confirmarse.</small>
                  }
                </td>
                <td class="col-cant">
                  <input type="number" min="1" max="10000" step="1" formControlName="cantidad" [attr.aria-label]="'Cantidad de la línea ' + (i + 1)" />
                  <app-campo-error [control]="linea.get('cantidad')" />
                </td>
                <td class="num">{{ precio(i) | currency: 'PEN' : 'S/' }}</td>
                <td class="num">{{ subtotal(i) | currency: 'PEN' : 'S/' }}</td>
                <td class="acciones">
                  <button type="button" class="btn btn-sm btn-claro" (click)="quitarLinea(i)" [disabled]="items.length === 1" aria-label="Quitar línea">Quitar</button>
                </td>
              </tr>
            }
          </tbody>
          <tfoot>
            <tr>
              <td colspan="3">
                <button type="button" class="btn btn-sm" (click)="agregarLinea()" [disabled]="items.length >= maxLineas">+ Agregar producto</button>
              </td>
              <td class="num total">{{ total() | currency: 'PEN' : 'S/' }}</td>
              <td></td>
            </tr>
          </tfoot>
        </table>
        <p class="texto-suave nota">El total final lo calcula el backend con el precio vigente de cada producto.</p>

        <div class="botones">
          <button class="btn btn-primario" type="submit" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : id() ? 'Guardar cambios' : 'Crear borrador' }}
          </button>
          <a class="btn btn-claro" [routerLink]="id() ? ['/pedidos', id()] : ['/pedidos']">Cancelar</a>
        </div>
      </form>
    }
  `,
})
export class PedidoForm implements OnInit {
  readonly id = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PedidoService);
  private readonly productoService = inject(ProductoService);
  private readonly clienteService = inject(ClienteService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  protected readonly maxLineas = MAX_LINEAS;
  protected readonly cargando = signal(true);
  protected readonly listo = signal(false);
  protected readonly guardando = signal(false);
  protected readonly bloqueado = signal<string | null>(null);
  protected readonly error = signal<ErrorUi | null>(null);
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly productos = signal<Producto[]>([]);

  protected readonly form = this.fb.group({
    clienteId: [null as number | null, Validators.required],
    items: this.fb.array([this.nuevaLinea()], [Validators.required, Validators.maxLength(MAX_LINEAS), sinRepetidos]),
  });

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly total = computed(() => {
    const items = this.valores().items ?? [];
    return items.reduce((s, it) => s + this.precioDe(it?.productoId) * (Number(it?.cantidad) || 0), 0);
  });

  get items(): FormArray<FormGroup> {
    return this.form.controls.items as FormArray<FormGroup>;
  }

  ngOnInit(): void {
    const id = Number(this.id());
    forkJoin({
      clientes: this.clienteService.listar(),
      productos: this.productoService.listar(),
      ...(id ? { pedido: this.service.obtener(id) } : {}),
    }).subscribe({
      next: (r) => {
        this.clientes.set(r.clientes);
        this.productos.set(r.productos);
        const pedido = (r as { pedido?: Pedido }).pedido;
        if (pedido) this.cargarPedido(pedido);
        this.listo.set(true);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.cargando.set(false);
      },
    });
  }

  agregarLinea(): void {
    if (this.items.length < MAX_LINEAS) this.items.push(this.nuevaLinea());
  }

  quitarLinea(i: number): void {
    if (this.items.length > 1) this.items.removeAt(i);
  }

  protected repetido(i: number): boolean {
    const id = this.items.at(i).get('productoId')?.value;
    return id != null && this.items.controls.some((g, j) => j < i && g.get('productoId')?.value === id);
  }

  protected sinStock(i: number): number | null {
    const v = this.items.at(i).value;
    const p = this.productos().find((x) => x.id === v.productoId);
    return p && Number(v.cantidad) > p.stock ? p.stock : null;
  }

  protected precio(i: number): number {
    return this.precioDe(this.items.at(i).value.productoId);
  }

  protected subtotal(i: number): number {
    return this.precio(i) * (Number(this.items.at(i).value.cantidad) || 0);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.items.hasError('repetidos')) {
        this.error.set({ status: 0, titulo: 'Revise el pedido', mensaje: 'Hay productos repetidos en varias líneas.', detalles: [] });
      }
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    const datos: PedidoRequest = {
      clienteId: Number(v.clienteId),
      items: v.items.map((it) => ({ productoId: Number(it['productoId']), cantidad: Number(it['cantidad']) })),
    };
    const id = Number(this.id());
    const op = id ? this.service.actualizar(id, datos) : this.service.crear(datos);
    op.subscribe({
      next: (p) => {
        this.avisos.exito(id ? `Pedido #${p.id} actualizado` : `Pedido #${p.id} creado en borrador`);
        this.router.navigate(['/pedidos', p.id]);
      },
      error: (e) => {
        const err = describirError(e);
        err.detalles = aplicarErroresServidor(this.form, err);
        this.error.set(err);
        this.guardando.set(false);
      },
    });
  }

  private cargarPedido(p: Pedido): void {
    if (p.estado !== 'BORRADOR') {
      this.bloqueado.set(p.estado);
      return;
    }
    this.items.clear();
    for (const it of p.items) this.items.push(this.nuevaLinea(it.productoId, it.cantidad));
    this.form.controls.clienteId.setValue(p.clienteId);
  }

  private nuevaLinea(productoId: number | null = null, cantidad: number | null = 1): FormGroup {
    return this.fb.group({
      productoId: [productoId, Validators.required],
      cantidad: [cantidad, [Validators.required, Validators.min(1), Validators.max(10000), entero]],
    });
  }

  private precioDe(productoId: unknown): number {
    return Number(this.productos().find((p) => p.id === productoId)?.precio ?? 0);
  }
}
