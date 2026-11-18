import { Injectable, signal } from '@angular/core';

export interface Notificacion {
  id: number;
  tipo: 'exito' | 'error' | 'info';
  texto: string;
}

/** Mensajes breves (toasts) que se cierran solos. */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private siguiente = 1;
  readonly lista = signal<Notificacion[]>([]);

  exito(texto: string): void {
    this.agregar('exito', texto);
  }

  error(texto: string): void {
    this.agregar('error', texto);
  }

  info(texto: string): void {
    this.agregar('info', texto);
  }

  cerrar(id: number): void {
    this.lista.update((l) => l.filter((n) => n.id !== id));
  }

  private agregar(tipo: Notificacion['tipo'], texto: string): void {
    const id = this.siguiente++;
    this.lista.update((l) => [...l, { id, tipo, texto }]);
    setTimeout(() => this.cerrar(id), tipo === 'error' ? 7000 : 4000);
  }
}
