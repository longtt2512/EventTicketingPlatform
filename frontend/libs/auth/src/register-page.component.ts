import { PasswordFieldComponent } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FORM_FEEDBACK, WORKSPACE, UnsavedChangesDirective } from '@event-ticketing/ui';
import { safeReturnUrl } from '@event-ticketing/session';
import { AuthApiService } from '@event-ticketing/api-client';
import { finalize } from 'rxjs';
import { errorMessage } from './feedback';

@Component({
    selector: 'etp-register-page', imports: [PasswordFieldComponent, ReactiveFormsModule, RouterLink, ...FORM_FEEDBACK, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <section class="auth-panel" aria-labelledby="register-title" [etpUnsavedChanges]="hasUnsavedChanges()"><h1 id="register-title">{{workspace==='checkin'?'Tạo tài khoản soát vé':workspace==='organizer'?'Đăng ký nhà tổ chức':'Tạo tài khoản'}}</h1>
      <p class="muted">{{workspace==='checkin'?'Sau khi xác thực email, nhờ chủ tổ chức thêm bạn làm thành viên Soát vé và phân công sự kiện. Đăng ký chưa cấp quyền check-in.':workspace==='organizer'?'Xác thực email rồi gửi hồ sơ nhà tổ chức để được xét duyệt. Một tài khoản có thể sử dụng nhiều vai trò.':workspace==='admin'?'Đăng ký tạo tài khoản cá nhân. Quyền quản trị phải được quản trị viên có thẩm quyền cấp.':'Tạo tài khoản để mua vé, theo dõi đơn hàng và tham gia sự kiện.'}}</p>
      @if (sent()) { <div class="notice success" role="status"><h2>Kiểm tra hộp thư</h2><p>Nếu địa chỉ đủ điều kiện, email xác thực sẽ được gửi tới bạn. Hãy mở liên kết trong email để hoàn tất.</p></div><p class="small">Kiểm tra cả thư rác. Nếu vẫn chưa nhận được, <a routerLink="/verify-email" [queryParams]="{returnUrl: returnUrl}">yêu cầu gửi lại email</a>.</p> }
      @else { <form etpForm [formGroup]="form" (ngSubmit)="submit()">
        <label for="displayName">Họ tên</label><input etpField id="displayName" formControlName="displayName" autocomplete="name" maxlength="160" required>
        <label for="email">Email</label><input etpField id="email" type="email" formControlName="email" autocomplete="email" maxlength="320" required>
        <label for="password">Mật khẩu</label><etp-password-field label="mật khẩu"><input etpField id="password" type="password" formControlName="password" autocomplete="new-password" minlength="10" maxlength="128" aria-describedby="password-help" required></etp-password-field>
        <small id="password-help">Sử dụng từ 10 đến 128 ký tự.</small>
        <label for="confirmPassword">Nhập lại mật khẩu</label><etp-password-field label="mật khẩu nhập lại"><input etpField [etpMatches]="form.controls.password" id="confirmPassword" type="password" formControlName="confirmPassword" autocomplete="new-password" required></etp-password-field>
        @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
        <button [disabled]="loading()">{{ loading() ? 'Đang tạo…' : 'Tạo tài khoản' }}</button>
      </form> }
      <p class="auth-links"><a class="back-link" routerLink="/login" [queryParams]="{returnUrl: returnUrl}">Quay lại đăng nhập</a></p>
    </section>`
})
export class RegisterPageComponent {
  private readonly api = inject(AuthApiService); private readonly destroy = inject(DestroyRef);
  readonly error = signal(''); readonly loading = signal(false); readonly sent = signal(false);
  readonly workspace = inject(WORKSPACE);
  readonly returnUrl = safeReturnUrl(inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl'));
  readonly form = inject(FormBuilder).nonNullable.group({
    displayName: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(160)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(128)]], confirmPassword: ['', Validators.required],
  });
  hasUnsavedChanges(): boolean { return !this.sent() && this.form.dirty; }
  submit(): void {
    if (this.form.invalid || this.loading()) return;
    const { displayName, email, password, confirmPassword } = this.form.getRawValue();
    if (password !== confirmPassword) { this.error.set('Mật khẩu nhập lại chưa khớp.'); return; }
    this.loading.set(true); this.error.set('');
    this.api.register({ displayName, email, password, locale: 'vi', context: { portal: this.workspace, returnUrl: this.returnUrl } }).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: () => { this.sent.set(true); this.form.reset(); }, error: error => this.error.set(errorMessage(error)),
    });
  }
}
