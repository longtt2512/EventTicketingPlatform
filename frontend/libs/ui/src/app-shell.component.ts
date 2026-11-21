import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthApiService } from '@event-ticketing/api-client';
import { SessionStore } from '@event-ticketing/session';
import { filter, finalize } from 'rxjs';
import { PendingEdits } from './unsaved-changes';
import { WORKSPACE, WORKSPACE_NAMES } from './workspace';
import { IconComponent } from './icon.component';

@Component({
    selector: 'etp-app-shell',
    imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet, IconComponent],
    changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './app-shell.component.html'
})
export class AppShellComponent {
  readonly session = inject(SessionStore);
  readonly workspace = inject(WORKSPACE);
  readonly workspaceName = WORKSPACE_NAMES[this.workspace];
  private readonly api = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly pendingEdits = inject(PendingEdits);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly path = signal(this.router.url.split('?')[0]);
  readonly loggingOut = signal(false);
  readonly logoutError = signal('');
  readonly menuOpen = signal(false);
  search = '';
  readonly authPage = computed(() => /^\/(?:auth\/)?(login|register|forgot-password|reset-password|verify-email)(?:$|\/)/.test(this.path()));
  readonly accountPage = computed(() => this.workspace === 'customer' && (this.path().startsWith('/account/') || this.path() === '/profile' || this.path() === '/apply'));
  readonly purchasePage = computed(() => /^\/(booking|checkout|payment)\//.test(this.path()));
  readonly initials = computed(() => (this.session.user()?.displayName ?? 'ETP').trim().split(/\s+/).slice(-2).map(s => s[0]).join('').toUpperCase());
  readonly portal = computed(() => this.workspace !== 'customer' && this.workspace !== 'checkin' && this.session.isAuthenticated() && (this.workspace !== 'admin' || !!this.session.user()?.roles.includes('ADMIN')));
  readonly currentSection = computed(() => {
    const p = this.path();
    if (p.includes('/analytics')) return 'Báo cáo doanh thu';
    if (p.includes('/settlements')) return 'Đối soát sự kiện';
    if (p.includes('/commission')) return 'Cấu hình phí';
    if (p.includes('/categories')) return 'Danh mục sự kiện';
    if (p.includes('/audit-logs')) return 'Nhật ký hoạt động';
    if (p.includes('/settings')) return 'Cấu hình vận hành';
    if (p.includes('/payment-exceptions')) return 'Ngoại lệ thanh toán';
    if (p.includes('/payments')) return 'Thanh toán';
    if (p.includes('/applications')) return 'Duyệt hồ sơ';
    if (p.includes('/users')) return 'Người dùng';
    if (p.includes('/check-in/')) return 'Check-in sự kiện';
    if (p.includes('/orders')) return 'Đơn hàng';
    if (p.includes('/events')) return this.workspace === 'admin' ? 'Kiểm duyệt sự kiện' : 'Sự kiện';
    if (p.includes('/organizations')) return 'Tổ chức';
    if (p.includes('/profile')) return 'Hồ sơ cá nhân';
    if (p.includes('/apply')) return 'Hồ sơ nhà tổ chức';
    return 'Tổng quan';
  });
  readonly navigation = computed(() => this.workspace === 'admin' ? [
    { group: 'ĐIỀU HÀNH', label: 'Tổng quan', path: '/home', icon: 'dashboard' },
    { group: '', label: 'Tài khoản người dùng', path: '/users', icon: 'users' },
    { group: '', label: 'Quản lý tổ chức', path: '/organizations', icon: 'building' },
    { group: '', label: 'Duyệt hồ sơ', path: '/applications', icon: 'user-check' },
    { group: 'SỰ KIỆN', label: 'Kiểm duyệt sự kiện', path: '/events', icon: 'calendar' },
    { group: '', label: 'Danh mục sự kiện', path: '/categories', icon: 'calendar' },
    { group: 'GIAO DỊCH', label: 'Đơn hàng', path: '/orders', icon: 'ticket' },
    { group: '', label: 'Đối soát sự kiện', path: '/settlements', icon: 'credit-card' },
    { group: '', label: 'Cấu hình phí', path: '/commission', icon: 'settings' },
    { group: '', label: 'Thanh toán', path: '/payments', icon: 'credit-card' },
    { group: '', label: 'Ngoại lệ thanh toán', path: '/payment-exceptions', icon: 'warning' },
    { group: 'VẬN HÀNH', label: 'Nhật ký hoạt động', path: '/audit-logs', icon: 'history' },
    { group: '', label: 'Cấu hình vận hành', path: '/settings', icon: 'settings' },
    { group: 'TÀI KHOẢN', label: 'Hồ sơ cá nhân', path: '/profile', icon: 'user' },
  ] : [
    { group: 'KHÔNG GIAN LÀM VIỆC', label: 'Tổng quan', path: '/home', icon: 'dashboard' },
    { group: '', label: 'Tổ chức của tôi', path: '/organizations', icon: 'building' },
    { group: '', label: 'Check-in sự kiện', path: '/check-in/events', icon: 'scan' },
    { group: 'TÀI KHOẢN', label: 'Hồ sơ nhà tổ chức', path: '/apply', icon: 'shield' },
    { group: '', label: 'Hồ sơ cá nhân', path: '/profile', icon: 'settings' },
  ]);

  constructor() {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => {
      this.path.set(this.router.url.split('?')[0]); this.menuOpen.set(false);
    });
  }
  findEvents(): void {
    void this.router.navigate(['/events'], { queryParams: { q: this.search.trim() || null } });
  }
  skipToContent(event: Event): void {
    // A plain fragment href resolves against <base href="/"> and can leave the current route.
    event.preventDefault();
    this.host.nativeElement.querySelector<HTMLElement>('main')?.focus();
  }
  logout(): void {
    if (this.loggingOut() || !this.pendingEdits.confirmDiscard()) return;
    this.loggingOut.set(true); this.logoutError.set('');
    const finish = () => {
      this.pendingEdits.allowNextNavigation(); this.session.clear();
      void this.router.navigateByUrl('/login').finally(() => this.pendingEdits.clearApproval());
    };
    this.api.logout().pipe(finalize(() => this.loggingOut.set(false))).subscribe({
      next: finish,
      error: () => this.logoutError.set('Chưa thể thu hồi phiên trên máy chủ. Hãy kiểm tra mạng và thử đăng xuất lại.'),
    });
  }
}
