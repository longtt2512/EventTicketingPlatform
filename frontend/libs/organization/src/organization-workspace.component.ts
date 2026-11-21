import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SessionStore } from '@event-ticketing/session';
import { Organization, OrganizationApi, statusLabel } from './organization-api';
import { errorMessage } from '../../auth/src/feedback';

/** The organization context remains visible on each of its management pages. */
@Component({
  selector: 'etp-organization-workspace', imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="back-link" routerLink="/organizations">Danh sách tổ chức</a>
    @if(error()){<p class="notice error" role="alert">{{error()}}</p><button class="secondary" type="button" (click)="load()">Thử lại</button>}
    @if(current();as org){
      <header class="page-heading"><h1>{{org.name}}</h1><span class="badge">{{label(org.status)}}</span></header>
      @if(org.status==='SUSPENDED'){<p class="notice warning">Tổ chức đã bị đình chỉ. Các thay đổi và quyền truy cập sự kiện của nhân viên đang bị chặn.</p>}
      <nav class="section-navigation" aria-label="Quản lý tổ chức">
        @for(item of sections();track item.id){
          @if(inline && !item.route){
            <button type="button" class="secondary" [attr.aria-current]="active===item.id?'page':null" (click)="sectionChange.emit(item.id)">{{item.name}}</button>
          }@else{
            <a class="button-link secondary" [routerLink]="item.route?['/organizations',org.id,item.route]:['/organizations',org.id]" [queryParams]="item.route?null:{section:item.id}" [attr.aria-current]="active===item.id?'page':null">{{item.name}}</a>
          }
        }
      </nav>
    }@else if(!error()){<p role="status">Đang tải thông tin tổ chức…</p>}
  `,
})
export class OrganizationWorkspaceComponent implements OnChanges {
  @Input() organizationId = '';
  @Input() organization: Organization | null = null;
  @Input() active = 'general';
  @Input() inline = false;
  @Output() sectionChange = new EventEmitter<string>();
  readonly current = signal<Organization | null>(null);
  readonly error = signal('');
  readonly label = statusLabel;
  private readonly api = inject(OrganizationApi);
  private readonly session = inject(SessionStore);
  private readonly destroy = inject(DestroyRef);
  ngOnChanges(): void {
    if (this.organization) { this.current.set(this.organization); return; }
    if (!this.organizationId || this.current()?.id === this.organizationId) return;
    this.load();
  }
  load(): void {
    this.error.set('');
    this.api.organization(this.organizationId).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: org => this.current.set(org), error: e => this.error.set(errorMessage(e)),
    });
  }
  sections(): { id: string; name: string; route?: string }[] {
    const org = this.current(), admin = this.session.user()?.roles.includes('ADMIN');
    const manage = admin || org?.owner || org?.permission === 'MANAGEMENT';
    const finance = admin || org?.owner || org?.permission === 'FINANCE';
    return [
      { id: 'general', name: 'Thông tin chung' },
      ...(manage ? [{ id: 'members', name: 'Thành viên' }] : []),
      ...(finance && !admin ? [{ id: 'analytics', name: 'Báo cáo doanh thu', route: 'analytics' }, { id: 'settlements', name: 'Đối soát', route: 'settlements' }] : []),
      ...(finance ? [{ id: 'payouts', name: 'Tài khoản nhận tiền' }] : []),
      ...(manage && !admin ? [{ id: 'events', name: 'Sự kiện và phân công' }] : []),
      ...(manage ? [{ id: 'audit', name: 'Lịch sử thao tác' }] : []),
    ];
  }
}
