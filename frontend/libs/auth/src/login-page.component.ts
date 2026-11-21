import { PasswordFieldComponent } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthApiService } from '@event-ticketing/api-client';
import { SessionStore, safeReturnUrl } from '@event-ticketing/session';
import { FORM_FEEDBACK } from '@event-ticketing/ui';
import { finalize } from 'rxjs';
import { errorMessage } from './feedback';

@Component({
    selector: 'etp-login-page', imports: [PasswordFieldComponent, ReactiveFormsModule, RouterLink, ...FORM_FEEDBACK], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <section class="auth-panel" aria-labelledby="login-title"><h1 id="login-title">Đăng nhập</h1>
      <p class="muted">Chào mừng bạn trở lại. Tiếp tục với tài khoản đã xác thực email.</p>
      @if (changed) { <p class="notice success" role="status">Mật khẩu đã được cập nhật. Hãy đăng nhập lại.</p> }
      @if (expired) { <p class="notice" role="status">Phiên của bạn đã hết hạn. Hãy đăng nhập để tiếp tục.</p> }
      <form etpForm [formGroup]="form" (ngSubmit)="submit()">
        <label for="email">Email</label><input etpField id="email" type="email" formControlName="email" autocomplete="email" maxlength="320" required>
        <label for="password">Mật khẩu</label><etp-password-field label="mật khẩu"><input etpField id="password" type="password" formControlName="password" autocomplete="current-password" maxlength="128" required></etp-password-field>
        @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
        <button [disabled]="loading()">{{ loading() ? 'Đang đăng nhập…' : 'Đăng nhập' }}</button>
      </form>
      <div class="auth-links"><a class="button-link quiet" routerLink="/forgot-password" [queryParams]="{returnUrl: returnUrl}">Quên mật khẩu?</a><a class="button-link quiet" routerLink="/register" [queryParams]="{returnUrl: returnUrl}">Tạo tài khoản</a></div>
      <p class="small recovery-hint">Chưa nhận được email xác thực? <a routerLink="/verify-email" [queryParams]="{returnUrl: returnUrl}">Gửi lại liên kết</a></p>
    </section>`
})
export class LoginPageComponent {
  private readonly api = inject(AuthApiService); private readonly session = inject(SessionStore);
  private readonly router = inject(Router); private readonly route = inject(ActivatedRoute); private readonly destroy = inject(DestroyRef);
  readonly changed = this.route.snapshot.queryParamMap.has('passwordChanged'); readonly expired = this.route.snapshot.queryParamMap.has('expired');
  readonly returnUrl = safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
  readonly error = signal(''); readonly loading = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.error.set(''); this.loading.set(true);
    this.api.login(this.form.getRawValue()).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: response => { this.session.setSession(response.accessToken, response.user); this.form.controls.password.reset(); void this.router.navigateByUrl(this.returnUrl); },
      error: error => this.error.set(errorMessage(error)),
    });
  }
}
