import { AfterViewInit, ChangeDetectorRef, Directive, DoCheck, ElementRef, HostListener, Input, OnChanges, OnDestroy, Renderer2, forwardRef, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { AbstractControl, FormGroupDirective, NG_VALIDATORS, NgControl, NgForm, ValidationErrors, Validator, Validators } from '@angular/forms';
import { VndInputDirective } from './vnd-input.directive';

/** One error vocabulary for both reactive and template-driven forms. */
export function fieldError(control: AbstractControl, label: string): string {
  if (control.hasError('eventRule')) return control.getError('eventRule');
  if (control.hasError('vnd')) return 'Nhập số tiền VNĐ nguyên, không âm, ví dụ 150.000.';
  if (control.hasError('min')) return `Nhập giá trị từ ${Number(control.getError('min').min).toLocaleString('vi-VN')} trở lên.`;
  if (control.hasError('max')) return `Nhập giá trị không vượt quá ${Number(control.getError('max').max).toLocaleString('vi-VN')}.`;
  if (control.hasError('required')) {
    const name=label.toLocaleLowerCase('vi');
    return name.startsWith('nhập ') ? `Vui lòng ${name}.` : `Vui lòng nhập ${name}.`;
  }
  if (control.hasError('email')) return 'Nhập email hợp lệ, ví dụ ten@example.com.';
  if (control.hasError('minlength')) return `Sử dụng ít nhất ${control.getError('minlength').requiredLength} ký tự.`;
  if (control.hasError('maxlength')) return `Chỉ nhập tối đa ${control.getError('maxlength').requiredLength} ký tự.`;
  if (control.hasError('passwordMismatch')) return 'Mật khẩu nhập lại chưa khớp.';
  if (control.hasError('pattern')) return label.toLocaleLowerCase('vi').includes('số tài khoản')
    ? 'Nhập từ 6 đến 34 chữ số, không có khoảng trắng.' : 'Kiểm tra định dạng và không để trống thông tin này.';
  return 'Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.';
}

@Directive({ selector: '[etpField]', standalone: true })
export class FieldFeedbackDirective implements AfterViewInit, DoCheck, OnDestroy {
  @Input() etpRequiredForReview = false;
  private requiredMarker?: HTMLElement;
  private readonly element = inject<ElementRef<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>>(ElementRef);
  private readonly ngControl = inject(NgControl, { self: true });
  private readonly renderer = inject(Renderer2);
  private errorElement?: HTMLElement;
  private originalDescription = '';
  private errorId = '';
  private previousMessage: string | undefined;

  ngAfterViewInit(): void {
    const field = this.element.nativeElement;
    this.originalDescription = field.getAttribute('aria-describedby') ?? '';
    this.errorId = `${field.id}-error`;
    this.errorElement = this.renderer.createElement('p') as HTMLElement;
    this.renderer.setAttribute(this.errorElement, 'id', this.errorId);
    this.renderer.setAttribute(this.errorElement, 'class', 'field-error');
    this.renderer.setAttribute(this.errorElement, 'aria-live', 'polite');
    this.renderer.insertBefore(field.parentNode, this.errorElement, field.nextSibling);
    this.render();
  }

  ngDoCheck(): void { this.render(); }
  @HostListener('blur') onBlur(): void { this.ngControl.control?.markAsTouched(); this.render(); }

  private render(): void {
    if (!this.errorElement) return;
    const field = this.element.nativeElement;
    const control = this.ngControl.control;
    const required = this.etpRequiredForReview || field.required || !!control?.hasValidator(Validators.required);
    const labelElement = field.labels?.[0];
    if (required && labelElement && !this.requiredMarker) {
      this.requiredMarker = this.renderer.createElement('span');
      this.renderer.setAttribute(this.requiredMarker, 'class', 'required-marker');
      this.renderer.setAttribute(this.requiredMarker, 'aria-hidden', 'true');
      // Keep the visual marker outside the label's accessible text.
      this.renderer.setProperty(this.requiredMarker, 'textContent', '');
      this.renderer.appendChild(labelElement, this.requiredMarker);
    } else if (!required && this.requiredMarker) { this.requiredMarker.remove(); this.requiredMarker = undefined; }
    this.renderer.setAttribute(field, 'aria-required', String(required));
    const label = labelElement?.textContent?.replace(/\s*\*$/, '').trim() || 'thông tin này';
    const message = control?.invalid && control.touched ? fieldError(control, label) : '';
    if (message === this.previousMessage) return;
    this.previousMessage = message;
    this.renderer.setProperty(this.errorElement, 'textContent', message);
    this.renderer.setProperty(this.errorElement, 'hidden', !message);
    this.renderer.setAttribute(field, 'aria-invalid', message ? 'true' : 'false');
    const description = [this.originalDescription, message ? this.errorId : ''].filter(Boolean).join(' ');
    if (description) this.renderer.setAttribute(field, 'aria-describedby', description);
    else this.renderer.removeAttribute(field, 'aria-describedby');
  }

  ngOnDestroy(): void { this.errorElement?.remove(); this.requiredMarker?.remove(); }
}

@Directive({ selector: 'form[etpForm]', standalone: true })
export class FormFeedbackDirective implements AfterViewInit, OnDestroy {
  private readonly element = inject<ElementRef<HTMLFormElement>>(ElementRef);
  private readonly reactive = inject(FormGroupDirective, { optional: true, self: true });
  private readonly template = inject(NgForm, { optional: true, self: true });
  private readonly changeDetector = inject(ChangeDetectorRef);
  private focusFrame?: number;
  private readonly submit = (event: Event) => {
    const form = this.reactive?.form ?? this.template?.form;
    if (!form || form.valid) return;
    form.markAllAsTouched();
    this.changeDetector.detectChanges();
    // Capture before ngSubmit: invalid template forms must never reach a mutation.
    event.preventDefault();
    event.stopImmediatePropagation();
    this.focusFrame = requestAnimationFrame(() => {
      this.element.nativeElement.querySelector<HTMLElement>('[etpField].ng-invalid:not(:disabled)')?.focus();
    });
  };
  ngAfterViewInit(): void { this.element.nativeElement.addEventListener('submit', this.submit, true); }
  ngOnDestroy(): void {
    this.element.nativeElement.removeEventListener('submit', this.submit, true);
    if (this.focusFrame !== undefined) cancelAnimationFrame(this.focusFrame);
  }
}

@Directive({
  selector: '[etpMatches]', standalone: true,
  providers: [{ provide: NG_VALIDATORS, useExisting: forwardRef(() => MatchingValueDirective), multi: true }],
})
export class MatchingValueDirective implements Validator, OnChanges, OnDestroy {
  @Input({ required: true }) etpMatches!: AbstractControl;
  private subscription?: Subscription;
  private changed = () => {};
  validate(control: AbstractControl): ValidationErrors | null {
    return control.value && control.value !== this.etpMatches?.value ? { passwordMismatch: true } : null;
  }
  registerOnValidatorChange(fn: () => void): void { this.changed = fn; }
  ngOnChanges(): void {
    this.subscription?.unsubscribe();
    this.subscription = this.etpMatches.valueChanges.subscribe(() => this.changed());
    this.changed();
  }
  ngOnDestroy(): void { this.subscription?.unsubscribe(); }
}

export const FORM_FEEDBACK = [FieldFeedbackDirective, FormFeedbackDirective, MatchingValueDirective, VndInputDirective] as const;
