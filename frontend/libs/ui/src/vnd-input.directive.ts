import { Directive, ElementRef, HostListener, Input, OnChanges, forwardRef, inject } from '@angular/core';
import { AbstractControl, ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';

const format = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
export function parseVnd(value: string): number | null {
  const text = value.trim().replace(/\s*(VNĐ|VND|₫|đ)$/i, '').trim();
  if (!text) return null;
  if (!/^(\d+|\d{1,3}(\.\d{3})+|\d{1,3}(,\d{3})+)$/.test(text)) return NaN;
  const amount = Number(text.replace(/[.,]/g, ''));
  return Number.isSafeInteger(amount) ? amount : NaN;
}

/** Display grouped dong while retaining a numeric API/form value. */
@Directive({
  selector: 'input[etpVnd]', standalone: true, host: { type: 'text', inputmode: 'numeric' },
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => VndInputDirective), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => VndInputDirective), multi: true },
  ],
})
export class VndInputDirective implements ControlValueAccessor, Validator, OnChanges {
  private readonly input = inject<ElementRef<HTMLInputElement>>(ElementRef);
  @Input() min: number | string = 0;
  @Input() max: number | string | null = null;
  private changed = (_: number | null) => {};
  private touched = () => {};
  private validationChanged = () => {};
  writeValue(value: number | string | null): void {
    this.input.nativeElement.value = value == null || value === '' ? '' : Number.isSafeInteger(Number(value)) ? format.format(Number(value)) : String(value);
  }
  registerOnChange(fn: (value: number | null) => void): void { this.changed = fn; }
  registerOnTouched(fn: () => void): void { this.touched = fn; }
  registerOnValidatorChange(fn: () => void): void { this.validationChanged = fn; }
  ngOnChanges(): void { this.validationChanged(); }
  setDisabledState(disabled: boolean): void { this.input.nativeElement.disabled = disabled; }
  validate(control: AbstractControl): ValidationErrors | null {
    const n = control.value;
    if (n == null || n === '') return null;
    if (!Number.isSafeInteger(Number(n)) || Number(n) < 0) return { vnd: true };
    if (Number(n) < Number(this.min)) return { min: { min: this.min, actual: n } };
    if (this.max != null && Number(n) > Number(this.max)) return { max: { max: this.max, actual: n } };
    return null;
  }
  @HostListener('input', ['$event']) onInput(event: InputEvent): void {
    const field = this.input.nativeElement;
    const digitsBefore = field.value.slice(0, field.selectionStart ?? field.value.length).replace(/\D/g, '').length;
    const editingDigits = event.inputType?.startsWith('delete') || (event.inputType === 'insertText' && /^\d+$/.test(event.data ?? ''));
    const value = parseVnd(editingDigits ? field.value.replace(/[.,]/g, '') : field.value);
    if (value == null || Number.isFinite(value)) {
      field.value = value == null ? '' : format.format(value);
      let position = 0, digits = 0;
      while (position < field.value.length && digits < digitsBefore) { if (/\d/.test(field.value[position])) digits++; position++; }
      field.setSelectionRange(position, position);
    }
    this.changed(value);
  }
  @HostListener('blur') onBlur(): void { this.touched(); }
}
