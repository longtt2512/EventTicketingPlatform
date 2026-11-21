import { ChangeDetectionStrategy, Component, ContentChild, ElementRef, Input, signal } from '@angular/core';
import { FieldFeedbackDirective } from './form-feedback.directive';

/** Preserve the native input and its Angular form control, validation and autofill. */
@Component({
  selector: 'etp-password-field', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content/><button class="password-toggle" type="button" [attr.aria-label]="(visible() ? 'Ẩn ' : 'Hiện ') + label" [attr.aria-pressed]="visible()" (click)="toggle()">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>
      @if(visible()){<path d="m3 3 18 18"/>}
    </svg></button>`,
})
export class PasswordFieldComponent {
  @Input() label = 'mật khẩu';
  @ContentChild(FieldFeedbackDirective, { read: ElementRef }) private input?: ElementRef<HTMLInputElement>;
  readonly visible = signal(false);
  toggle(): void {
    const field = this.input?.nativeElement;
    if (!field || field.disabled) return;
    const start = field.selectionStart, end = field.selectionEnd;
    this.visible.update(value => !value);
    field.type = this.visible() ? 'text' : 'password';
    if (start !== null && end !== null) field.setSelectionRange(start, end);
  }
}
