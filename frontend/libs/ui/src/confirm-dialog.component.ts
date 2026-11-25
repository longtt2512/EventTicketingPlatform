import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild, signal } from '@angular/core';

export interface Confirmation {
  title: string;
  subject: string;
  description: string;
  confirmLabel: string;
  reason?: string;
  danger?: boolean;
  reasonRequired?: boolean;
}

@Component({
  selector: 'etp-confirm-dialog', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<dialog #dialog class="confirm-dialog" aria-labelledby="confirmation-title" aria-describedby="confirmation-description" (cancel)="cancel($event)" (keydown)="trapTab($event)">
    <h2 id="confirmation-title">{{ confirmation.title }}</h2>
    <p class="confirmation-subject">{{ confirmation.subject }}</p>
    <p id="confirmation-description">{{ confirmation.description }}</p>
    @if (confirmation.reasonRequired) {
      <div class="confirmation-input"><label for="confirmation-reason">Lý do thay đổi trạng thái <span class="required-marker" aria-hidden="true"></span></label>
        <textarea #reasonField id="confirmation-reason" maxlength="1000" required aria-describedby="confirmation-reason-help" [attr.aria-invalid]="reasonError()" (input)="reason = reasonField.value; reasonError.set(false)"></textarea>
        <p id="confirmation-reason-help" class="small">Bắt buộc nhập lý do để lưu vào lịch sử của danh mục này.</p>
        @if(reasonError()){<p class="field-error" role="alert">Vui lòng nhập lý do thay đổi trạng thái.</p>}
      </div>
    }
    @if (confirmation.reason && !confirmation.reasonRequired) { <div class="confirmation-reason"><strong>Lý do / ghi chú</strong><p>{{ confirmation.reason }}</p></div> }
    <div class="actions"><button #cancelButton type="button" class="secondary" (click)="resolved.emit(false)">Hủy</button>
      <button type="button" [class.danger]="confirmation.danger" (click)="confirm()">{{ confirmation.confirmLabel }}</button></div>
  </dialog>`,
})
export class ConfirmDialogComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) confirmation!: Confirmation;
  reason = '';
  readonly reasonError = signal(false);
  @Output() reasonSubmitted = new EventEmitter<string>();
  @Output() resolved = new EventEmitter<boolean>();
  @ViewChild('dialog', { static: true }) private dialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('cancelButton', { static: true }) private cancelButton!: ElementRef<HTMLButtonElement>;
  private previousFocus: HTMLElement | null = null;
  ngAfterViewInit(): void {
    this.previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.dialog.nativeElement.showModal();
    this.cancelButton.nativeElement.focus();
  }
  confirm(): void {
    if (this.confirmation.reasonRequired && !this.reason.trim()) {
      this.reasonError.set(true);
      this.dialog.nativeElement.querySelector('textarea')?.focus();
      return;
    }
    this.reasonSubmitted.emit(this.reason.trim());
    this.resolved.emit(true);
  }
  cancel(event: Event): void { event.preventDefault(); this.resolved.emit(false); }
  trapTab(event: KeyboardEvent): void {
    if (event.key !== 'Tab') return;
    const controls = this.dialog.nativeElement.querySelectorAll<HTMLElement>('button:not(:disabled),textarea:not(:disabled)');
    const first = controls[0], last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
  ngOnDestroy(): void {
    this.dialog.nativeElement.close();
    if (this.previousFocus?.isConnected) this.previousFocus.focus();
  }
}

/** Capture the reviewed subject and payload before opening the dialog. */
export class ConfirmationState {
  readonly current = signal<Confirmation | null>(null);
  private action?: () => void;
  ask(confirmation: Confirmation, action: () => void): void {
    if (this.current()) return;
    this.action = action;
    this.current.set(confirmation);
  }
  resolve(accepted: boolean): void {
    const action = this.action;
    this.action = undefined;
    this.current.set(null);
    if (accepted) action?.();
  }
}
