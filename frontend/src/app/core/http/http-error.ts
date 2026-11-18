import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiError } from '../models/api.models';

/** Error HTTP traducido a un mensaje que entiende el usuario. */
export interface ErrorUi {
  status: number;
  titulo: string;
  mensaje: string;
  detalles: string[];
}

const TITULOS: Record<number, string> = {
  0: 'Sin conexión con el servidor',
  400: 'Datos inválidos (400)',
  401: 'No autenticado (401)',
  403: 'Acceso denegado (403)',
  404: 'No encontrado (404)',
  409: 'Conflicto (409)',
  500: 'Error del servidor (500)',
};

const MENSAJES: Record<number, string> = {
  0: 'No se pudo contactar al backend. Verifique que Spring Boot esté en ejecución en el puerto 8080.',
  400: 'Revise los datos enviados.',
  401: 'Su sesión no es válida o venció. Inicie sesión nuevamente.',
  403: 'Su rol no tiene permiso para realizar esta operación.',
  404: 'El registro solicitado no existe o fue eliminado.',
  409: 'La operación entra en conflicto con el estado actual de los datos.',
  500: 'Ocurrió un error inesperado en el servidor.',
};

export function describirError(err: unknown): ErrorUi {
  if (!(err instanceof HttpErrorResponse)) {
    return { status: -1, titulo: 'Error', mensaje: String(err), detalles: [] };
  }
  const cuerpo = (typeof err.error === 'object' && err.error ? err.error : null) as Partial<ApiError> | null;
  const status = err.status;
  return {
    status,
    titulo: TITULOS[status] ?? `Error ${status}`,
    mensaje: cuerpo?.message || MENSAJES[status] || err.message,
    detalles: Array.isArray(cuerpo?.errors) ? cuerpo.errors : [],
  };
}

/**
 * Asocia los errores 400 del backend ("campo: mensaje") a los controles del formulario,
 * para mostrarlos debajo de cada campo. Devuelve los detalles que no se pudieron asociar.
 */
export function aplicarErroresServidor(form: FormGroup, error: ErrorUi): string[] {
  if (error.status !== 400) return error.detalles;
  const sinCampo: string[] = [];
  for (const det of error.detalles) {
    const idx = det.indexOf(':');
    // "items[0].cantidad" (Spring) -> "items.0.cantidad" (ruta de Angular Forms)
    const campo = idx > 0 ? det.slice(0, idx).trim().replace(/\[(\d+)\]/g, '.$1') : '';
    const control: AbstractControl | null = campo ? form.get(campo) : null;
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), servidor: det.slice(idx + 1).trim() });
      control.markAsTouched();
    } else {
      sinCampo.push(det);
    }
  }
  return sinCampo;
}
