import { Directive, HostListener, Input, Injectable, OnDestroy, inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

export interface UnsavedChanges { hasUnsavedChanges(): boolean; }
export const discardChangesMessage = 'Bạn có thay đổi chưa lưu. Rời trang sẽ bỏ các thông tin đang nhập. Bạn có muốn rời trang?';
export const unsavedChangesGuard: CanDeactivateFn<UnsavedChanges> = component =>
  inject(PendingEdits).consumeApproval() || !component.hasUnsavedChanges() || window.confirm(discardChangesMessage);

/** Browser reload/close is protected without storing passwords or bank details. */
@Directive({ selector: '[etpUnsavedChanges]', standalone: true })
export class UnsavedChangesDirective implements OnDestroy {
  private readonly pending = inject(PendingEdits);
  private readonly check = () => this.etpUnsavedChanges;
  constructor() { this.pending.register(this.check); }
  ngOnDestroy(): void { this.pending.unregister(this.check); }
  @Input() etpUnsavedChanges = false;
  @HostListener('window:beforeunload', ['$event']) beforeUnload(event: BeforeUnloadEvent): void {
    if (this.etpUnsavedChanges) { event.preventDefault(); event.returnValue = ''; }
  }
}

@Injectable({ providedIn: 'root' })
export class PendingEdits {
  private approved = false;
  allowNextNavigation(): void { this.approved = true; }
  clearApproval(): void { this.approved = false; }
  consumeApproval(): boolean { const value = this.approved; this.approved = false; return value; }
  private readonly checks = new Set<() => boolean>();
  register(check: () => boolean): void { this.checks.add(check); }
  unregister(check: () => boolean): void { this.checks.delete(check); }
  hasChanges(): boolean { return [...this.checks].some(check => check()); }
  confirmDiscard(): boolean { return ![...this.checks].some(check => check()) || window.confirm(discardChangesMessage); }
}
