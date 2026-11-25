import { refreshOnReturn } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FORM_FEEDBACK, ConfirmDialogComponent, ConfirmationState, UnsavedChangesDirective, discardChangesMessage } from '@event-ticketing/ui';
import { User } from '@event-ticketing/models';
import { errorMessage } from '@event-ticketing/auth';
import { finalize } from 'rxjs';

@Component({
    selector: 'etp-admin-users', imports: [ReactiveFormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div [etpUnsavedChanges]="hasUnsavedChanges()"><div class="toolbar"><div class="page-heading"><h1>Tài khoản người dùng</h1><p>Khóa hoặc mở khóa tài khoản. Các thay đổi được lưu lịch sử.</p></div>@if(error()){<button class="secondary" (click)="load()" [disabled]="loading()">Thử lại</button>}</div>
    @if (error()) { <p role="alert" class="notice error">{{ error() }}</p> }
    @if (message()) { <p role="status" class="notice success">{{ message() }}</p> }
    @if (selected(); as user) {
      <section class="panel" aria-labelledby="status-title"><h2 id="status-title">{{ user.status === 'LOCKED' ? 'Mở khóa tài khoản' : 'Khóa tài khoản' }}</h2><p>{{ user.displayName }} · {{ user.email }}</p>
        <form etpForm [formGroup]="form" (ngSubmit)="change()"><label for="reason">Lý do</label><textarea etpField id="reason" formControlName="reason" maxlength="2000" [required]="user.status !== 'LOCKED'"></textarea>
          <div class="actions"><button [disabled]="saving()">{{ saving() ? 'Đang lưu…' : 'Xác nhận thay đổi' }}</button><button type="button" class="secondary" (click)="cancelSelection()" [disabled]="saving()">Hủy</button></div>
        </form>
      </section>
    }
    <section class="panel">
      @if (loading()) { <p role="status">Đang tải tài khoản…</p> }
      <div class="table-scroll"><table><caption class="small">Danh sách tài khoản và trạng thái truy cập</caption><thead><tr><th>Họ tên</th><th>Email</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
        @for (user of users(); track user.id) { <tr><td>{{ user.displayName }}</td><td>{{ user.email }}</td><td><span class="badge">{{ user.status }}</span></td><td><button type="button" class="secondary" (click)="select(user)" [disabled]="saving()">{{ user.status === 'LOCKED' ? 'Mở khóa' : 'Khóa' }}</button></td></tr> }
      </tbody></table></div>
      @if (!loading() && !users().length) { <p class="empty-state">Chưa có tài khoản.</p> }
      @if (nextCursor()) { <button class="secondary" (click)="load(true)" [disabled]="loading()">Xem thêm</button> }
    </section></div>@if(confirmation.current();as decision){<etp-confirm-dialog [confirmation]="decision" (resolved)="confirmation.resolve($event)"/>}`
})
export class UsersPageComponent {
 private readonly destroy=inject(DestroyRef);
  private readonly http = inject(HttpClient);
  readonly users = signal<User[]>([]); readonly selected = signal<User | null>(null); readonly nextCursor = signal<string | null>(null);
  readonly loading = signal(false); readonly saving = signal(false); readonly error = signal(''); readonly message = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({ reason: ['', Validators.maxLength(2000)] });
  readonly confirmation = new ConfirmationState();
  hasUnsavedChanges(): boolean { return this.form.dirty; }
  cancelSelection(): void { if (!this.hasUnsavedChanges() || window.confirm(discardChangesMessage)) { this.selected.set(null); this.form.reset(); } }
  constructor() {refreshOnReturn(()=>void this.load(),()=>this.loading()||this.hasUnsavedChanges()); this.load(); }
  select(user: User): void { if (this.hasUnsavedChanges() && !window.confirm(discardChangesMessage)) return; this.selected.set(user); this.form.reset(); this.message.set(''); this.error.set(''); }
  load(more = false): void {
    this.loading.set(true); this.error.set('');
    const query = more && this.nextCursor() ? '?cursor=' + encodeURIComponent(this.nextCursor()!) : '';
    this.http.get<{items: User[]; nextCursor: string | null}>('/api/v1/admin/users' + query).pipe(takeUntilDestroyed(this.destroy),finalize(() => this.loading.set(false))).subscribe({
      next: result => { this.users.set(more ? [...this.users(), ...result.items] : result.items); this.nextCursor.set(result.nextCursor); }, error: error => this.error.set(errorMessage(error)),
    });
  }
  change(): void {
    const user = this.selected(); if (!user || this.saving() || this.form.invalid) return;
    const locking=user.status!=='LOCKED', reason=this.form.controls.reason.value.trim();
    if(locking&&!reason) { this.form.controls.reason.setErrors({required:true}); this.form.markAllAsTouched(); return; }
    this.confirmation.ask({title:locking?'Khóa tài khoản?':'Mở khóa tài khoản?',subject:user.displayName+' · '+user.email,
      description:locking?'Người dùng sẽ mất quyền truy cập, kể cả với phiên đăng nhập hiện tại.':'Người dùng có thể đăng nhập lại bằng tài khoản này.',reason,
      confirmLabel:locking?'Khóa tài khoản':'Mở khóa tài khoản',danger:locking},()=>{
      this.saving.set(true);this.error.set('');
      this.http.post('/api/v1/admin/users/'+user.id+(locking?'/lock':'/unlock'),{reason}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.saving.set(false))).subscribe({
        next:()=>{this.selected.set(null);this.form.reset();this.message.set('Đã cập nhật trạng thái tài khoản.');this.load();},error:error=>this.error.set(errorMessage(error)),
      });
    });
  }
}
