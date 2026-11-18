import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EstadoPedido, Pedido, PedidoRequest } from '../models/api.models';

/** /api/pedidos */
@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/pedidos';

  listar(filtro: { clienteId?: number | null; estado?: EstadoPedido | '' } = {}): Observable<Pedido[]> {
    let params = new HttpParams();
    if (filtro.clienteId) params = params.set('clienteId', filtro.clienteId);
    if (filtro.estado) params = params.set('estado', filtro.estado);
    return this.http.get<Pedido[]>(this.url, { params });
  }

  obtener(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.url}/${id}`);
  }

  crear(p: PedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.url, p);
  }

  actualizar(id: number, p: PedidoRequest): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.url}/${id}`, p);
  }

  confirmar(id: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${id}/confirmar`, null);
  }

  cancelar(id: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${id}/cancelar`, null);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
