import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { Producto } from '../../core/models/api.models';
import { NotificacionService } from '../../core/notificacion.service';
import { ProductoService } from '../../core/services/producto.service';
import { ErrorApi } from '../../shared/error-api/error-api';

@Component({
  selector: 'app-producto-lista',
  imports: [RouterLink, ReactiveFormsModule, CurrencyPipe, ErrorApi],
  template: `
    <div class="encabezado">
      <h1>Productos</h1>
      @if (auth.puedeAdministrarCatalogo()) {
        <a class="btn btn-primario" routerLink="/productos/nuevo">Nuevo producto</a>
      }
    </div>

    <form class="filtros" (ngSubmit)="buscar()">
      <input [formControl]="texto" placeholder="Buscar por nombre o SKU" aria-label="Buscar productos" />
      <button class="btn" type="submit">Buscar</button>
      @if (texto.value) {
        <button class="btn btn-claro" type="button" (click)="texto.setValue(''); buscar()">Limpiar</button>
      }
    </form>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else {
      <div class="tarjeta sin-relleno">
        <table class="tabla">
          <thead>
            <tr>
              <th>SKU</th><th>Nombre</th><th>Categoría</th><th class="num">Precio</th><th class="num">Stock</th>
              @if (auth.puedeAdministrarCatalogo()) { <th class="acciones">Acciones</th> }
            </tr>
          </thead>
          <tbody>
            @for (p of productos(); track p.id) {
              <tr>
                <td><code>{{ p.sku }}</code></td>
                <td>{{ p.nombre }}</td>
                <td>{{ p.categoria }}</td>
                <td class="num">{{ p.precio | currency: 'PEN' : 'S/' }}</td>
                <td class="num" [class.alerta-texto]="p.stock === 0">{{ p.stock }}</td>
                @if (auth.puedeAdministrarCatalogo()) {
                  <td class="acciones">
                    <a class="btn btn-sm" [routerLink]="['/productos', p.id, 'editar']">Editar</a>
                    <button class="btn btn-sm btn-peligro" type="button" (click)="eliminar(p)">Eliminar</button>
                  </td>
                }
              </tr>
            } @empty {
              <tr><td colspan="6" class="vacio">No hay productos que coincidan.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class ProductoLista implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(ProductoService);
  private readonly avisos = inject(NotificacionService);

  protected readonly texto = new FormControl('', { nonNullable: true });
  protected readonly productos = signal<Producto[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<ErrorUi | null>(null);

  ngOnInit(): void {
    this.buscar();
  }

  buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.service.listar(this.texto.value.trim()).subscribe({
      next: (l) => {
        this.productos.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.cargando.set(false);
      },
    });
  }

  eliminar(p: Producto): void {
    if (!confirm(`¿Eliminar el producto ${p.sku} - ${p.nombre}?`)) return;
    this.error.set(null);
    this.service.eliminar(p.id).subscribe({
      next: () => {
        this.avisos.exito(`Producto ${p.sku} eliminado`);
        this.productos.update((l) => l.filter((x) => x.id !== p.id));
      },
      error: (e) => this.error.set(describirError(e)),
    });
  }
}
