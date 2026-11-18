import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Cliente, ClienteRequest } from '../models/api.models';

/** /api/clientes */
@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/clientes';

  listar(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.url);
  }

  obtener(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.url}/${id}`);
  }

  crear(c: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(this.url, c);
  }

  actualizar(id: number, c: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.url}/${id}`, c);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
