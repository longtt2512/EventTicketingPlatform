import { refreshOnReturn } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { Organization, OrganizationApi, statusLabel } from './organization-api';
@Component({
    selector: 'etp-organizations', imports: [RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `<header class="page-heading"><h1>Tổ chức</h1><p>Các tổ chức trong phạm vi quyền của bạn.</p></header>
 @if(error()){<p class="notice error" role="alert">{{error()}}</p>}@if(error()){<button class="secondary" [disabled]="busy()" (click)="load()">Thử lại</button>}
 @if(busy()){<p role="status">Đang tải…</p>} <div class="organization-grid">@for(o of organizations();track o.id){<section class="panel organization-card"><div class="organization-mark" aria-hidden="true">{{o.name.charAt(0)}}</div><h2><a [routerLink]="['/organizations',o.id]">{{o.name}}</a></h2><p class="status-badge" [attr.data-state]="o.status">{{label(o.status)}}</p><p>{{o.address}}</p></section>}@empty{@if(!busy()){<section class="panel empty-state"><h2>Chưa có tổ chức</h2><p>Tổ chức sẽ xuất hiện sau khi hồ sơ được duyệt hoặc bạn được thêm làm thành viên.</p></section>}}</div>`
})
export class OrganizationsPageComponent {
 readonly api=inject(OrganizationApi);readonly organizations=signal<Organization[]>([]);readonly error=signal('');readonly busy=signal(false);readonly label=statusLabel;
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy());this.load();}
 load(){this.busy.set(true);this.error.set('');this.api.organizations().pipe(finalize(()=>this.busy.set(false))).subscribe({next:o=>this.organizations.set(o),error:e=>this.error.set(errorMessage(e))});}
}
