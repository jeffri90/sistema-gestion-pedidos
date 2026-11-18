import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { Cliente, EstadoPedido, Pedido } from '../../core/models/api.models';
import { ClienteService } from '../../core/services/cliente.service';
import { PedidoService } from '../../core/services/pedido.service';
import { ErrorApi } from '../../shared/error-api/error-api';
import { EstadoBadge } from './estado-badge';

@Component({
  selector: 'app-pedido-lista',
  imports: [RouterLink, ReactiveFormsModule, CurrencyPipe, DatePipe, ErrorApi, EstadoBadge],
  template: `
    <div class="encabezado">
      <h1>Pedidos</h1>
      @if (auth.puedeGestionarPedidos()) {
        <a class="btn btn-primario" routerLink="/pedidos/nuevo">Nuevo pedido</a>
      }
    </div>

    <form class="filtros" [formGroup]="filtros" (ngSubmit)="buscar()">
      <select formControlName="clienteId" aria-label="Cliente">
        <option [ngValue]="null">Todos los clientes</option>
        @for (c of clientes(); track c.id) {
          <option [ngValue]="c.id">{{ c.razonSocial }}</option>
        }
      </select>
      <select formControlName="estado" aria-label="Estado">
        <option value="">Todos los estados</option>
        <option value="BORRADOR">Borrador</option>
        <option value="CONFIRMADO">Confirmado</option>
        <option value="CANCELADO">Cancelado</option>
      </select>
      <button class="btn" type="submit">Filtrar</button>
    </form>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else {
      <div class="tarjeta sin-relleno">
        <table class="tabla">
          <thead>
            <tr><th>N.°</th><th>Fecha</th><th>Cliente</th><th>Estado</th><th class="num">Líneas</th><th class="num">Total</th><th></th></tr>
          </thead>
          <tbody>
            @for (p of pedidos(); track p.id) {
              <tr>
                <td>#{{ p.id }}</td>
                <td>{{ p.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ p.cliente }}</td>
                <td><app-estado-badge [estado]="p.estado" /></td>
                <td class="num">{{ p.items.length }}</td>
                <td class="num">{{ p.total | currency: 'PEN' : 'S/' }}</td>
                <td class="acciones"><a class="btn btn-sm" [routerLink]="['/pedidos', p.id]">Ver detalle</a></td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="vacio">No hay pedidos con esos filtros.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class PedidoLista implements OnInit {
  /** ?clienteId=… al llegar desde la lista de clientes. */
  readonly clienteId = input<string>();

  protected readonly auth = inject(AuthService);
  private readonly service = inject(PedidoService);
  private readonly clienteService = inject(ClienteService);
  private readonly router = inject(Router);

  protected readonly filtros = inject(FormBuilder).nonNullable.group({
    clienteId: [null as number | null],
    estado: ['' as EstadoPedido | ''],
  });
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<ErrorUi | null>(null);

  ngOnInit(): void {
    const id = Number(this.clienteId());
    if (id) this.filtros.controls.clienteId.setValue(id);
    this.clienteService.listar().subscribe({
      next: (l) => this.clientes.set(l),
      error: (e) => this.error.set(describirError(e)),
    });
    this.buscar();
  }

  buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const f = this.filtros.getRawValue();
    this.router.navigate([], { queryParams: { clienteId: f.clienteId || null }, replaceUrl: true });
    this.service.listar(f).subscribe({
      next: (l) => {
        this.pedidos.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.cargando.set(false);
      },
    });
  }
}
