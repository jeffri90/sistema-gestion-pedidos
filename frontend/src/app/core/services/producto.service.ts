import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Producto, ProductoRequest } from '../models/api.models';

/** /api/productos */
@Injectable({ providedIn: 'root' })
export class ProductoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/productos';

  listar(texto = ''): Observable<Producto[]> {
    const params = texto ? new HttpParams().set('texto', texto) : undefined;
    return this.http.get<Producto[]>(this.url, { params });
  }

  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.url}/${id}`);
  }

  crear(p: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.url, p);
  }

  actualizar(id: number, p: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.url}/${id}`, p);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
