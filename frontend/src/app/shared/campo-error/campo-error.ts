import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

/** Mensaje de validación debajo de un campo, visible cuando el usuario ya lo tocó. */
@Component({
  selector: 'app-campo-error',
  template: `
    @if (mensaje(); as m) {
      <small class="campo-error" role="alert">{{ m }}</small>
    }
  `,
})
export class CampoError {
  readonly control = input.required<AbstractControl | null>();
  /** Texto para el error "pattern", que depende de cada campo. */
  readonly patron = input('Formato inválido');

  mensaje(): string | null {
    const c = this.control();
    if (!c || !c.errors || !(c.touched || c.dirty)) return null;
    const e = c.errors;
    if (e['required']) return 'Este campo es obligatorio';
    if (e['servidor']) return e['servidor'];
    if (e['pattern']) return this.patron();
    if (e['email']) return 'Ingrese un correo válido';
    if (e['minlength']) return `Mínimo ${e['minlength'].requiredLength} caracteres`;
    if (e['maxlength']) return `Máximo ${e['maxlength'].requiredLength} caracteres`;
    if (e['min']) return `El valor mínimo es ${e['min'].min}`;
    if (e['max']) return `El valor máximo es ${e['max'].max}`;
    if (e['decimales']) return 'Use como máximo 2 decimales';
    if (e['entero']) return 'Ingrese un número entero';
    if (e['repetido']) return 'Este producto ya está en otra línea';
    if (e['stock']) return `Solo hay ${e['stock']} unidades en stock`;
    return 'Valor inválido';
  }
}
