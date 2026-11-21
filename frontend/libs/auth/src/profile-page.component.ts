import { PasswordFieldComponent } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { FORM_FEEDBACK, UnsavedChangesDirective } from '@event-ticketing/ui';
import { Router } from '@angular/router';
import { AuthApiService } from '@event-ticketing/api-client';
import { SessionStore } from '@event-ticketing/session';
import { finalize } from 'rxjs';
import { errorMessage } from './feedback';

@Component({
    selector: 'etp-profile-page', imports: [PasswordFieldComponent, ReactiveFormsModule, ...FORM_FEEDBACK, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div class="page-heading"><h1>Hồ sơ cá nhân</h1><p>Cập nhật thông tin và bảo vệ tài khoản của bạn.</p></div>
    <nav class="profile-tabs" aria-label="Cài đặt tài khoản"><button class="quiet" [attr.aria-current]="tab()==='profile'?'page':null" (click)="tab.set('profile')">Thông tin cá nhân</button><button class="quiet" [attr.aria-current]="tab()==='security'?'page':null" (click)="tab.set('security')">Bảo mật</button></nav><div class="profile-settings" [etpUnsavedChanges]="hasUnsavedChanges()">
      <section class="panel" aria-labelledby="profile-title" [hidden]="tab()!=='profile'">
        <h2 id="profile-title">Thông tin cá nhân</h2>
        <p class="muted">{{ session.user()?.email }}</p>
        <p class="small wrap">Mã người dùng: <code>{{ session.user()?.id }}</code></p>
        <form etpForm [formGroup]="profile" (ngSubmit)="save()">
          <label for="display-name">Họ tên</label><input etpField id="display-name" formControlName="displayName" autocomplete="name" maxlength="160" required>
          <label for="locale">Ngôn ngữ ưu tiên</label><select etpField id="locale" formControlName="preferredLocale"><option value="vi">Tiếng Việt</option><option value="en">English</option></select>
          @if (profileError()) { <p role="alert" class="notice error">{{ profileError() }}</p> }
          @if (saved()) { <p role="status" class="notice success">Đã lưu hồ sơ.</p> }
          <button [disabled]="saving()">{{ saving() ? 'Đang lưu…' : 'Lưu hồ sơ' }}</button>
        </form>
      </section>
      <section class="panel" aria-labelledby="password-title" [hidden]="tab()!=='security'">
        <h2 id="password-title">Đổi mật khẩu</h2><p class="muted">Sau khi đổi, bạn cần đăng nhập lại trên các thiết bị.</p>
        <form etpForm [formGroup]="password" (ngSubmit)="changePassword()">
          <label for="current-password">Mật khẩu hiện tại</label><etp-password-field label="mật khẩu hiện tại"><input etpField id="current-password" type="password" formControlName="currentPassword" autocomplete="current-password" required></etp-password-field>
          <label for="new-password">Mật khẩu mới</label><etp-password-field label="mật khẩu mới"><input etpField id="new-password" type="password" formControlName="newPassword" autocomplete="new-password" required minlength="10" maxlength="128" aria-describedby="password-help"></etp-password-field>
          <small id="password-help">Sử dụng từ 10 đến 128 ký tự.</small>
          <label for="confirm-password">Nhập lại mật khẩu mới</label><etp-password-field label="mật khẩu mới"><input etpField [etpMatches]="password.controls.newPassword" id="confirm-password" type="password" formControlName="confirmPassword" autocomplete="new-password" required></etp-password-field>
          @if (passwordError()) { <p role="alert" class="notice error">{{ passwordError() }}</p> }
          <button [disabled]="changing()">{{ changing() ? 'Đang đổi…' : 'Đổi mật khẩu' }}</button>
        </form>
      </section>
    </div>`
})
export class ProfilePageComponent {
 private readonly destroy=inject(DestroyRef);
  readonly session = inject(SessionStore); private readonly api = inject(AuthApiService);
  private readonly fb = inject(FormBuilder); private readonly router = inject(Router);
  readonly tab = signal<'profile'|'security'>('profile');
  readonly saving = signal(false); readonly changing = signal(false); readonly saved = signal(false);
  readonly profileError = signal(''); readonly passwordError = signal('');
  readonly profile = this.fb.nonNullable.group({ displayName: [this.session.user()?.displayName ?? '', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(160)]], preferredLocale: [this.session.user()?.preferredLocale ?? 'vi', Validators.required] });
  readonly password = this.fb.nonNullable.group({ currentPassword: ['', Validators.required], newPassword: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(128)]], confirmPassword: ['', Validators.required] });
  hasUnsavedChanges(): boolean { return this.profile.dirty || this.password.dirty; }
  save(): void {
    if (this.profile.invalid || this.saving()) return;
    this.saving.set(true); this.profileError.set(''); this.saved.set(false);
    this.api.updateProfile(this.profile.getRawValue()).pipe(takeUntilDestroyed(this.destroy),finalize(() => this.saving.set(false))).subscribe({
      next: user => { this.session.updateUser(user); this.profile.markAsPristine(); this.saved.set(true); }, error: error => this.profileError.set(errorMessage(error)),
    });
  }
  changePassword(): void {
    if (this.password.invalid || this.changing()) return;
    const value = this.password.getRawValue();
    if (value.newPassword !== value.confirmPassword) { this.passwordError.set('Mật khẩu nhập lại chưa khớp.'); return; }
    this.changing.set(true); this.passwordError.set('');
    this.api.changePassword(value.currentPassword, value.newPassword).pipe(takeUntilDestroyed(this.destroy),finalize(() => this.changing.set(false))).subscribe({
      next: () => { this.password.reset(); this.session.clear(); void this.router.navigate(['/login'], { queryParams: { passwordChanged: '1', returnUrl: '/profile' } }); },
      error: error => this.passwordError.set(errorMessage(error)),
    });
  }
}
