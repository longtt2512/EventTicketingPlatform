import { refreshOnReturn } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { EventDraft, OrganizationApi, statusLabel } from './organization-api';
@Component({
    selector: 'etp-staff-events', imports: [RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `<header class="page-heading"><h1>Sự kiện được phân công</h1><p>Chỉ các phân công còn hiệu lực tại tổ chức đang hoạt động được hiển thị.</p></header>
 @if(error()){<p class="notice error" role="alert">{{error()}}</p>}@if(message()){<p class="notice success" role="status">{{message()}}</p>}
 @if(error()){<button class="secondary" [disabled]="busy()" (click)="load()">Thử lại</button>}@if(busy()){<p role="status">Đang tải…</p>}
 @for(e of events();track e.id){<section class="panel"><h2>{{e.name}}</h2><p class="badge">{{label(e.status)}}</p><div class="actions"><button [disabled]="busy()" (click)="check(e.id)">Xem phân công</button><a class="button-link secondary" [routerLink]="['/check-in/events',e.id,'scan']">Mở soát vé</a></div></section>}@empty{@if(!busy()){<section class="panel empty-state"><h2>Chưa có sự kiện được phân công</h2><p>Liên hệ chủ tổ chức nếu bạn cần được cấp quyền.</p></section>}}`
})
export class StaffEventsComponent {
 readonly api=inject(OrganizationApi);readonly events=signal<EventDraft[]>([]);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=statusLabel;
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy());this.load();}
 load(){this.busy.set(true);this.error.set('');this.message.set('');this.api.staffEvents().pipe(finalize(()=>this.busy.set(false))).subscribe({next:e=>this.events.set(e),error:e=>this.error.set(errorMessage(e))});}
 check(id:string){this.busy.set(true);this.error.set('');this.message.set('');this.api.staffEvent(id).pipe(finalize(()=>this.busy.set(false))).subscribe({next:e=>this.message.set('Bạn đang được phân công tại '+e.name+'.'),error:e=>this.error.set(errorMessage(e))});}
}
