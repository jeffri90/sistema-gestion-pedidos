import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Reglas de validación compartidas; replican las anotaciones de los DTO del backend. */
export const PATRONES = {
  sku: /^[A-Za-z0-9-]{2,30}$/,
  ruc: /^[0-9]{11}$/,
  telefono: /^[+0-9 ()-]{7,20}$/,
};

export function maxDecimales(n: number): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = c.value;
    if (v === null || v === undefined || v === '') return null;
    const [, dec = ''] = String(v).split('.');
    return dec.length > n ? { decimales: true } : null;
  };
}

export const entero: ValidatorFn = (c) => {
  const v = c.value;
  if (v === null || v === undefined || v === '') return null;
  return Number.isInteger(Number(v)) ? null : { entero: true };
};

/** Quita espacios al inicio y al final de los campos de texto. */
export function limpiar<T extends object>(valor: T): T {
  const out = { ...valor } as Record<string, unknown>;
  for (const k of Object.keys(out)) {
    if (typeof out[k] === 'string') out[k] = (out[k] as string).trim();
  }
  return out as T;
}
