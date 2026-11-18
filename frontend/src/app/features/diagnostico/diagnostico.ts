import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Observable, forkJoin, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { SKIP_AUTH } from '../../core/auth/auth.interceptor';
import { ErrorUi, describirError } from '../../core/http/http-error';
import { ClienteService } from '../../core/services/cliente.service';
import { ProductoService } from '../../core/services/producto.service';
import { ErrorApi } from '../../shared/error-api/error-api';

interface Caso {
  codigo: number;
  titulo: string;
  peticion: string;
  explicacion: string;
  ejecutar: () => Observable<unknown>;
}

interface Resultado {
  codigo: number;
  ok: boolean;
  error: ErrorUi | null;
}

/**
 * Página de evidencia: envía al backend peticiones reales que fallan a propósito
 * y muestra cómo la interfaz presenta cada respuesta. No modifica datos.
 */
@Component({
  selector: 'app-diagnostico',
  imports: [ErrorApi],
  template: `
    <div class="encabezado">
      <div>
        <h1>Manejo de errores HTTP</h1>
        <p class="texto-suave">
          Cada botón envía una petición real a Spring Boot que debe fallar. Ninguna modifica la base de datos.
          Su rol actual es <strong>{{ auth.rolPrincipal() }}</strong>.
        </p>
      </div>
    </div>

    <div class="casos">
      @for (c of casos; track c.codigo) {
        <section class="tarjeta caso">
          <header>
            <span class="codigo">{{ c.codigo }}</span>
            <div>
              <h2>{{ c.titulo }}</h2>
              <code>{{ c.peticion }}</code>
            </div>
          </header>
          <p class="texto-suave">{{ c.explicacion }}</p>
          <button class="btn" type="button" (click)="probar(c)" [disabled]="enCurso() === c.codigo">
            {{ enCurso() === c.codigo ? 'Enviando…' : 'Enviar petición' }}
          </button>
          @if (resultado(c.codigo); as r) {
            @if (r.error) {
              @if (r.error.status !== c.codigo) {
                <p class="campo-aviso">Se esperaba {{ c.codigo }} y el backend respondió {{ r.error.status }} (depende de su rol).</p>
              }
              <app-error-api [error]="r.error" />
            } @else {
              <div class="alerta alerta-info">La petición no falló (respuesta 2xx).</div>
            }
          }
        </section>
      }
    </div>
  `,
})
export class Diagnostico {
  protected readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly productos = inject(ProductoService);
  private readonly clientes = inject(ClienteService);

  protected readonly enCurso = signal<number | null>(null);
  private readonly resultados = signal<Record<number, Resultado>>({});

  protected readonly casos: Caso[] = [
    {
      codigo: 400,
      titulo: 'Datos inválidos',
      peticion: 'POST /api/pedidos  { clienteId: null, items: [] }',
      explicacion: 'Bean Validation rechaza el cuerpo y devuelve la lista de campos con error. Requiere rol ADMIN o VENTAS (CONSULTA recibe 403).',
      ejecutar: () => this.http.post('/api/pedidos', { clienteId: null, items: [] }),
    },
    {
      codigo: 401,
      titulo: 'Token inválido',
      peticion: 'GET /api/productos  (Authorization: Bearer token-invalido)',
      explicacion: 'Se envía un JWT falso. El backend lo rechaza; esta prueba no cierra su sesión real.',
      ejecutar: () =>
        this.http.get('/api/productos', {
          headers: new HttpHeaders({ Authorization: 'Bearer token-invalido' }),
          context: new HttpContext().set(SKIP_AUTH, true),
        }),
    },
    {
      codigo: 403,
      titulo: 'Rol sin permiso',
      peticion: 'DELETE /api/pedidos/999999',
      explicacion: 'Eliminar es exclusivo de ADMIN. Con VENTAS o CONSULTA se obtiene 403; con ADMIN, 404 porque el pedido no existe.',
      ejecutar: () => this.http.delete('/api/pedidos/999999'),
    },
    {
      codigo: 404,
      titulo: 'Registro inexistente',
      peticion: 'GET /api/productos/999999',
      explicacion: 'Se consulta un producto que no existe.',
      ejecutar: () => this.http.get('/api/productos/999999'),
    },
    {
      codigo: 409,
      titulo: 'Regla de negocio',
      peticion: 'POST /api/pedidos  (mismo producto en dos líneas)',
      explicacion: 'El servicio rechaza un pedido con productos repetidos y la transacción no guarda nada. Requiere ADMIN o VENTAS.',
      ejecutar: () =>
        forkJoin({ p: this.productos.listar(), c: this.clientes.listar() }).pipe(
          switchMap(({ p, c }) =>
            !p.length || !c.length
              ? throwError(() => new Error('Se necesita al menos un producto y un cliente'))
              : this.http.post('/api/pedidos', {
                  clienteId: c[0].id,
                  items: [
                    { productoId: p[0].id, cantidad: 1 },
                    { productoId: p[0].id, cantidad: 1 },
                  ],
                }),
          ),
        ),
    },
  ];

  protected resultado(codigo: number): Resultado | undefined {
    return this.resultados()[codigo];
  }

  probar(c: Caso): void {
    this.enCurso.set(c.codigo);
    c.ejecutar().subscribe({
      next: () => this.guardar(c.codigo, { codigo: c.codigo, ok: true, error: null }),
      error: (e) => this.guardar(c.codigo, { codigo: c.codigo, ok: false, error: describirError(e) }),
    });
  }

  private guardar(codigo: number, r: Resultado): void {
    this.resultados.update((m) => ({ ...m, [codigo]: r }));
    this.enCurso.set(null);
  }
}
