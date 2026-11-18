import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { Cliente, Pedido, Producto } from '../../core/models/api.models';
import { ClienteService } from '../../core/services/cliente.service';
import { PedidoService } from '../../core/services/pedido.service';
import { ProductoService } from '../../core/services/producto.service';
import { ErrorApi } from '../../shared/error-api/error-api';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, CurrencyPipe, ErrorApi],
  template: `
    <div class="encabezado">
      <div>
        <h1>Resumen</h1>
        <p class="texto-suave">Datos leídos en vivo desde la API REST de Spring Boot.</p>
      </div>
      @if (auth.puedeGestionarPedidos()) {
        <a class="btn btn-primario" routerLink="/pedidos/nuevo">Nuevo pedido</a>
      }
    </div>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else if (!error()) {
      <div class="metricas">
        <a class="tarjeta metrica" routerLink="/productos">
          <span class="metrica-valor">{{ productos().length }}</span>
          <span>Productos</span>
        </a>
        <a class="tarjeta metrica" routerLink="/clientes">
          <span class="metrica-valor">{{ clientes().length }}</span>
          <span>Clientes</span>
        </a>
        <a class="tarjeta metrica" routerLink="/pedidos">
          <span class="metrica-valor">{{ borradores() }}</span>
          <span>Pedidos en borrador</span>
        </a>
        <div class="tarjeta metrica">
          <span class="metrica-valor">{{ ventas() | currency: 'PEN' : 'S/' }}</span>
          <span>Total confirmado</span>
        </div>
      </div>

      <div class="grilla-2">
        <section class="tarjeta">
          <h2>Stock bajo (menos de 20)</h2>
          @if (stockBajo().length) {
            <table class="tabla">
              <thead><tr><th>SKU</th><th>Producto</th><th class="num">Stock</th></tr></thead>
              <tbody>
                @for (p of stockBajo(); track p.id) {
                  <tr><td>{{ p.sku }}</td><td>{{ p.nombre }}</td><td class="num">{{ p.stock }}</td></tr>
                }
              </tbody>
            </table>
          } @else {
            <p class="texto-suave">Todos los productos tienen stock suficiente.</p>
          }
        </section>

        <section class="tarjeta">
          <h2>Permisos de su rol: {{ auth.rolPrincipal() }}</h2>
          <ul class="permisos">
            <li [class.si]="true">Consultar productos, clientes y pedidos</li>
            <li [class.si]="auth.puedeGestionarPedidos()">Crear, editar, confirmar y cancelar pedidos</li>
            <li [class.si]="auth.puedeAdministrarCatalogo()">Crear y editar productos y clientes</li>
            <li [class.si]="auth.puedeEliminar()">Eliminar registros</li>
          </ul>
        </section>
      </div>
    }
  `,
})
export class Inicio implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly productoService = inject(ProductoService);
  private readonly clienteService = inject(ClienteService);
  private readonly pedidoService = inject(PedidoService);

  protected readonly cargando = signal(true);
  protected readonly error = signal<ErrorUi | null>(null);
  protected readonly productos = signal<Producto[]>([]);
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly pedidos = signal<Pedido[]>([]);

  protected readonly borradores = computed(() => this.pedidos().filter((p) => p.estado === 'BORRADOR').length);
  protected readonly ventas = computed(() =>
    this.pedidos()
      .filter((p) => p.estado === 'CONFIRMADO')
      .reduce((s, p) => s + Number(p.total), 0),
  );
  protected readonly stockBajo = computed(() => this.productos().filter((p) => p.stock < 20));

  ngOnInit(): void {
    forkJoin({
      productos: this.productoService.listar(),
      clientes: this.clienteService.listar(),
      pedidos: this.pedidoService.listar(),
    }).subscribe({
      next: (r) => {
        this.productos.set(r.productos);
        this.clientes.set(r.clientes);
        this.pedidos.set(r.pedidos);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.cargando.set(false);
      },
    });
  }
}
