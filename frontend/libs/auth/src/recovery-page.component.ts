import { PasswordFieldComponent } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { AuthApiService } from '@event-ticketing/api-client';
import { combineLatest, filter, finalize, interval } from 'rxjs';
import { FORM_FEEDBACK, WORKSPACE, UnsavedChangesDirective } from '@event-ticketing/ui';
import { safeReturnUrl } from '@event-ticketing/session';
import { errorMessage } from './feedback';

@Component({
    selector: 'etp-recovery-page', imports: [PasswordFieldComponent, ReactiveFormsModule, RouterLink, ...FORM_FEEDBACK, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <section class="auth-panel" [etpUnsavedChanges]="hasUnsavedChanges()"><h1>{{ title }}</h1>
      @if (mode === 'forgot') {
        <p class="muted">Nhập email tài khoản để nhận liên kết đặt lại mật khẩu.</p>
        @if (!done()) { <form etpForm [formGroup]="forgot" (ngSubmit)="requestReset()">
          <label for="email">Email</label><input etpField id="email" type="email" formControlName="email" autocomplete="email" maxlength="320" required>
          <button [disabled]="loading()">{{ loading() ? 'Đang gửi…' : 'Gửi liên kết' }}</button>
        </form> }
      }
      @if (mode === 'reset' && !done() && token) {
        <p class="muted">Chọn mật khẩu mới cho tài khoản của bạn.</p>
        <form etpForm [formGroup]="reset" (ngSubmit)="resetPassword()">
          <label for="new-password">Mật khẩu mới</label><etp-password-field label="mật khẩu mới"><input etpField id="new-password" type="password" formControlName="newPassword" autocomplete="new-password" minlength="10" maxlength="128" required aria-describedby="password-help"></etp-password-field>
          <small id="password-help">Sử dụng từ 10 đến 128 ký tự.</small>
          <label for="confirm-password">Nhập lại mật khẩu mới</label><etp-password-field label="mật khẩu mới"><input etpField [etpMatches]="reset.controls.newPassword" id="confirm-password" type="password" formControlName="confirmPassword" autocomplete="new-password" required></etp-password-field>
          <button [disabled]="loading()">{{ loading() ? 'Đang cập nhật…' : 'Đặt lại mật khẩu' }}</button>
        </form>
      }
      @if (mode === 'verify' && !done() && token) { <p class="muted">Xác nhận địa chỉ email để bắt đầu sử dụng tài khoản.</p><button (click)="verifyEmail()" [disabled]="loading()">{{ loading() ? 'Đang xác thực…' : 'Xác thực email' }}</button> }
      @if (mode === 'verify' && !done()) {
        @if (token) { <p class="recovery-hint"><button class="secondary" type="button" (click)="requestNewLink()" [disabled]="loading()">Yêu cầu liên kết mới</button></p> }
        @else {
          <p class="muted">Nhập email đã đăng ký để nhận liên kết xác thực mới. Kiểm tra cả thư mục thư rác.</p>
          <form etpForm [formGroup]="forgot" (ngSubmit)="resendVerification()">
            <label for="verification-email">Email đã đăng ký</label><input etpField id="verification-email" type="email" formControlName="email" autocomplete="email" maxlength="320" required>
            <button [disabled]="loading() || remaining() > 0">{{ loading() ? 'Đang gửi…' : 'Gửi lại email xác thực' }}</button>
            @if (remaining() > 0) { <p class="small" role="timer" aria-live="off">Bạn có thể yêu cầu gửi lại sau {{ remaining() }} giây.</p> }
            @if (resendSent()) { <p class="notice success" role="status">Nếu email thuộc tài khoản cần xác thực và đủ thời gian chờ, liên kết sẽ được gửi tới hộp thư đó.</p> }
          </form>
        }
      }
      @if (error()) { <p role="alert" class="notice error">{{ error() }}</p> }
      @if (done()) { <p role="status" class="notice success">{{ successMessage }}</p> }
      <div class="auth-links"><a class="back-link" routerLink="/login" [queryParams]="{returnUrl: returnUrl}">Quay lại đăng nhập</a>@if (mode === 'reset') { <a class="button-link quiet" routerLink="/forgot-password" [queryParams]="{returnUrl: returnUrl}">Yêu cầu liên kết mới</a> }</div>
    </section>`
})
export class RecoveryPageComponent {
  private readonly api = inject(AuthApiService); private readonly route = inject(ActivatedRoute); private readonly destroy = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly mode = this.route.snapshot.data['mode'] as 'forgot' | 'reset' | 'verify';
  readonly workspace = inject(WORKSPACE);
  readonly returnUrl = safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
  hasUnsavedChanges(): boolean { return !this.done() && (this.forgot.dirty || this.reset.dirty); }
  token = '';
  readonly title = this.mode === 'forgot' ? 'Quên mật khẩu' : this.mode === 'reset' ? 'Đặt lại mật khẩu' : 'Xác thực email';
  readonly successMessage = this.mode === 'forgot' ? 'Nếu email thuộc một tài khoản, liên kết đặt lại mật khẩu sẽ được gửi tới hộp thư đó.' : this.mode === 'reset' ? 'Đã đặt lại mật khẩu. Bạn có thể đăng nhập với mật khẩu mới.' : 'Email đã được xác thực. Bạn có thể đăng nhập.';
  readonly remaining = signal(0); readonly resendSent = signal(false); private resendUntil = 0;
  readonly error = signal(''); readonly done = signal(false); readonly loading = signal(false);
  readonly forgot = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });
  readonly reset = this.fb.nonNullable.group({ newPassword: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(128)]], confirmPassword: ['', Validators.required] });
  constructor() {
    if (this.mode === 'verify') interval(1000).pipe(takeUntilDestroyed(this.destroy)).subscribe(() => this.remaining.set(Math.max(0, Math.ceil((this.resendUntil - Date.now()) / 1000))));
    if (this.mode !== 'forgot') {
      combineLatest([this.route.fragment, this.route.queryParamMap]).pipe(takeUntilDestroyed(this.destroy)).subscribe(([fragment, query]) => {
        const value = new URLSearchParams(fragment ?? '').get('token') ?? query.get('token') ?? '';
        if (value) { this.token = value; this.error.set(''); this.done.set(false); }
        if (this.mode === 'reset' && !this.token && !this.done()) this.error.set('Liên kết không có mã xác thực. Hãy mở liên kết từ email hoặc yêu cầu liên kết mới.');
      });
      this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd), takeUntilDestroyed(this.destroy)).subscribe(event => {
        const url = this.router.parseUrl(event.urlAfterRedirects);
        if (new URLSearchParams(url.fragment ?? '').has('token') || url.queryParams['token']) {
          url.fragment = null; delete url.queryParams['token'];
          void this.router.navigateByUrl(url, { replaceUrl: true });
        }
      });
    }
  }
  requestNewLink(): void { this.token = ''; this.error.set(''); }
  resendVerification(): void {
    if (this.mode !== 'verify' || this.forgot.invalid || this.loading() || this.remaining() > 0) return;
    this.loading.set(true); this.error.set(''); this.resendSent.set(false);
    this.api.resendVerification(this.forgot.controls.email.value, { portal: this.workspace, returnUrl: this.returnUrl }).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: response => {
        this.resendUntil = Date.now() + response.retryAfterSeconds * 1000;
        this.remaining.set(response.retryAfterSeconds); this.resendSent.set(true); this.forgot.markAsPristine();
      }, error: error => this.error.set(errorMessage(error)),
    });
  }
  requestReset(): void {
    if (this.forgot.invalid || this.loading()) return;
    this.perform(this.api.forgotPassword(this.forgot.controls.email.value, { portal: this.workspace, returnUrl: this.returnUrl }));
  }
  resetPassword(): void {
    if (this.reset.invalid || this.loading() || !this.token) return;
    const value = this.reset.getRawValue();
    if (value.newPassword !== value.confirmPassword) { this.error.set('Mật khẩu nhập lại chưa khớp.'); return; }
    this.perform(this.api.resetPassword(this.token, value.newPassword));
  }
  verifyEmail(): void { if (this.token && !this.loading()) this.perform(this.api.verifyEmail(this.token)); }
  private perform(request: import('rxjs').Observable<void>): void {
    this.loading.set(true); this.error.set('');
    request.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: () => { this.done.set(true); this.token = ''; this.reset.reset(); }, error: error => { this.error.set(errorMessage(error)); if (this.mode === 'verify' && error?.error?.code === 'INVALID_VERIFICATION_TOKEN') this.token = ''; },
    });
  }
}
