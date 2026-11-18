import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { Pedido } from '../../core/models/api.models';
import { NotificacionService } from '../../core/notificacion.service';
import { PedidoService } from '../../core/services/pedido.service';
import { ErrorApi } from '../../shared/error-api/error-api';
import { EstadoBadge } from './estado-badge';

@Component({
  selector: 'app-pedido-detalle',
  imports: [RouterLink, CurrencyPipe, DatePipe, ErrorApi, EstadoBadge],
  template: `
    <div class="encabezado">
      <h1>Pedido #{{ id() }}</h1>
      <a class="btn btn-claro" routerLink="/pedidos">Volver a pedidos</a>
    </div>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else if (pedido(); as p) {
      <section class="tarjeta">
        <dl class="datos">
          <div><dt>Cliente</dt><dd>{{ p.cliente }}</dd></div>
          <div><dt>Fecha</dt><dd>{{ p.fecha | date: 'dd/MM/yyyy HH:mm' }}</dd></div>
          <div><dt>Estado</dt><dd><app-estado-badge [estado]="p.estado" /></dd></div>
          <div><dt>Total</dt><dd class="total">{{ p.total | currency: 'PEN' : 'S/' }}</dd></div>
        </dl>

        <table class="tabla">
          <thead><tr><th>Producto</th><th class="num">Cantidad</th><th class="num">Precio unit.</th><th class="num">Subtotal</th></tr></thead>
          <tbody>
            @for (it of p.items; track it.productoId) {
              <tr>
                <td>{{ it.producto }}</td>
                <td class="num">{{ it.cantidad }}</td>
                <td class="num">{{ it.precioUnitario | currency: 'PEN' : 'S/' }}</td>
                <td class="num">{{ it.subtotal | currency: 'PEN' : 'S/' }}</td>
              </tr>
            }
          </tbody>
        </table>

        <div class="botones">
          @if (auth.puedeGestionarPedidos() && p.estado === 'BORRADOR') {
            <a class="btn" [routerLink]="['/pedidos', p.id, 'editar']">Editar</a>
            <button class="btn btn-primario" type="button" [disabled]="ocupado()" (click)="confirmar(p)">Confirmar y descontar stock</button>
          }
          @if (auth.puedeGestionarPedidos() && p.estado !== 'CANCELADO') {
            <button class="btn btn-advertencia" type="button" [disabled]="ocupado()" (click)="cancelar(p)">Cancelar pedido</button>
          }
          @if (auth.puedeEliminar()) {
            <button class="btn btn-peligro" type="button" [disabled]="ocupado()" (click)="eliminar(p)">Eliminar</button>
          }
        </div>
        @if (p.estado === 'CONFIRMADO') {
          <p class="texto-suave nota">Un pedido confirmado debe cancelarse (devuelve el stock) antes de eliminarlo.</p>
        }
      </section>
    }
  `,
})
export class PedidoDetalle implements OnInit {
  readonly id = input.required<string>();

  protected readonly auth = inject(AuthService);
  private readonly service = inject(PedidoService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  protected readonly pedido = signal<Pedido | null>(null);
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<ErrorUi | null>(null);

  ngOnInit(): void {
    this.service.obtener(Number(this.id())).subscribe({
      next: (p) => {
        this.pedido.set(p);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.cargando.set(false);
      },
    });
  }

  confirmar(p: Pedido): void {
    if (!confirm(`¿Confirmar el pedido #${p.id}? Se descontará el stock.`)) return;
    this.ejecutar(this.service.confirmar(p.id), `Pedido #${p.id} confirmado`);
  }

  cancelar(p: Pedido): void {
    const aviso = p.estado === 'CONFIRMADO' ? ' Se devolverá el stock.' : '';
    if (!confirm(`¿Cancelar el pedido #${p.id}?${aviso}`)) return;
    this.ejecutar(this.service.cancelar(p.id), `Pedido #${p.id} cancelado`);
  }

  eliminar(p: Pedido): void {
    if (!confirm(`¿Eliminar definitivamente el pedido #${p.id}?`)) return;
    this.ocupado.set(true);
    this.error.set(null);
    this.service.eliminar(p.id).subscribe({
      next: () => {
        this.avisos.exito(`Pedido #${p.id} eliminado`);
        this.router.navigate(['/pedidos']);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.ocupado.set(false);
      },
    });
  }

  private ejecutar(op: Observable<Pedido>, mensaje: string): void {
    this.ocupado.set(true);
    this.error.set(null);
    op.subscribe({
      next: (p) => {
        this.pedido.set(p);
        this.avisos.exito(mensaje);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.ocupado.set(false);
      },
    });
  }
}
