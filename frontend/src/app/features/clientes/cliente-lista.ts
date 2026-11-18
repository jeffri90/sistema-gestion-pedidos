import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { Cliente } from '../../core/models/api.models';
import { NotificacionService } from '../../core/notificacion.service';
import { ClienteService } from '../../core/services/cliente.service';
import { ErrorApi } from '../../shared/error-api/error-api';

@Component({
  selector: 'app-cliente-lista',
  imports: [RouterLink, ReactiveFormsModule, ErrorApi],
  template: `
    <div class="encabezado">
      <h1>Clientes</h1>
      @if (auth.puedeAdministrarCatalogo()) {
        <a class="btn btn-primario" routerLink="/clientes/nuevo">Nuevo cliente</a>
      }
    </div>

    <div class="filtros">
      <input [formControl]="filtro" placeholder="Filtrar por RUC o razón social" aria-label="Filtrar clientes" />
    </div>

    <app-error-api [error]="error()" />

    @if (cargando()) {
      <p class="texto-suave">Cargando…</p>
    } @else {
      <div class="tarjeta sin-relleno">
        <table class="tabla">
          <thead>
            <tr>
              <th>RUC</th><th>Razón social</th><th>Correo</th><th>Teléfono</th><th>Dirección</th>
              <th class="acciones">Acciones</th>
            </tr>
          </thead>
          <tbody>
            @for (c of visibles(); track c.id) {
              <tr>
                <td><code>{{ c.ruc }}</code></td>
                <td>{{ c.razonSocial }}</td>
                <td>{{ c.email }}</td>
                <td>{{ c.telefono }}</td>
                <td>{{ c.direccion }}</td>
                <td class="acciones">
                  <a class="btn btn-sm btn-claro" [routerLink]="['/pedidos']" [queryParams]="{ clienteId: c.id }">Pedidos</a>
                  @if (auth.puedeAdministrarCatalogo()) {
                    <a class="btn btn-sm" [routerLink]="['/clientes', c.id, 'editar']">Editar</a>
                  }
                  @if (auth.puedeEliminar()) {
                    <button class="btn btn-sm btn-peligro" type="button" (click)="eliminar(c)">Eliminar</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="vacio">No hay clientes que coincidan.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class ClienteLista implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(ClienteService);
  private readonly avisos = inject(NotificacionService);

  protected readonly filtro = new FormControl('', { nonNullable: true });
  private readonly textoFiltro = toSignal(this.filtro.valueChanges, { initialValue: '' });
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<ErrorUi | null>(null);

  protected readonly visibles = computed(() => {
    const t = this.textoFiltro().trim().toLowerCase();
    return t
      ? this.clientes().filter((c) => c.ruc.includes(t) || c.razonSocial.toLowerCase().includes(t))
      : this.clientes();
  });

  ngOnInit(): void {
    this.service.listar().subscribe({
      next: (l) => {
        this.clientes.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(describirError(e));
        this.cargando.set(false);
      },
    });
  }

  eliminar(c: Cliente): void {
    if (!confirm(`¿Eliminar al cliente ${c.razonSocial}?`)) return;
    this.error.set(null);
    this.service.eliminar(c.id).subscribe({
      next: () => {
        this.avisos.exito(`Cliente ${c.razonSocial} eliminado`);
        this.clientes.update((l) => l.filter((x) => x.id !== c.id));
      },
      error: (e) => this.error.set(describirError(e)),
    });
  }
}
