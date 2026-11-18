import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, Rol } from '../models/api.models';

interface Sesion {
  username: string;
  token: string;
  roles: Rol[];
  /** Instante de vencimiento en milisegundos. */
  expiresAt: number;
}

const STORAGE_KEY = 'pedidos.sesion';

/**
 * Inicia y cierra sesión contra /api/auth/login y guarda el JWT en sessionStorage
 * (se borra al cerrar la pestaña). El token se envía en authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly sesion = signal<Sesion | null>(this.leer());

  readonly username = computed(() => this.sesion()?.username ?? '');
  readonly roles = computed(() => this.sesion()?.roles ?? []);
  readonly rolPrincipal = computed(() => this.roles()[0] ?? '');

  login(datos: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', datos).pipe(
      tap((r) => {
        const s: Sesion = {
          username: datos.username,
          token: r.token,
          roles: r.roles,
          expiresAt: Date.now() + r.expiresIn * 1000,
        };
        this.guardar(s);
      }),
    );
  }

  logout(motivo?: 'expirada'): void {
    this.guardar(null);
    this.router.navigate(['/login'], motivo ? { queryParams: { sesion: motivo } } : {});
  }

  token(): string | null {
    const s = this.sesion();
    if (!s) return null;
    if (Date.now() >= s.expiresAt) {
      this.guardar(null);
      return null;
    }
    return s.token;
  }

  autenticado(): boolean {
    return this.token() !== null;
  }

  tieneRol(...roles: Rol[]): boolean {
    return this.roles().some((r) => roles.includes(r));
  }

  /** Permisos de la interfaz; replican SecurityConfig del backend. */
  puedeAdministrarCatalogo(): boolean {
    return this.tieneRol('ADMIN');
  }

  puedeGestionarPedidos(): boolean {
    return this.tieneRol('ADMIN', 'VENTAS');
  }

  puedeEliminar(): boolean {
    return this.tieneRol('ADMIN');
  }

  private guardar(s: Sesion | null): void {
    this.sesion.set(s);
    try {
      if (s) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(s));
      else sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      /* almacenamiento no disponible: la sesión vive solo en memoria */
    }
  }

  private leer(): Sesion | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const s = JSON.parse(raw) as Sesion;
      return s.expiresAt > Date.now() ? s : null;
    } catch {
      return null;
    }
  }
}
