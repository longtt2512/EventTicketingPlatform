import { refreshOnReturn } from '@event-ticketing/ui';
import { EventImageComponent } from '../../events/src/event-image.component';
import {ChangeDetectionStrategy,Component,DestroyRef,inject,signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {TicketApi,ticketError} from './ticket-api';
import {EventAccess} from './ticket-models';
import {dateTime,label} from '../../events/src/event-models';
@Component({
    selector: 'etp-checkin-events', imports: [RouterLink, EventImageComponent], changeDetection: ChangeDetectionStrategy.OnPush, template: `<header class="page-heading"><h1>Check-in sự kiện</h1><p>Chọn đúng sự kiện trước khi quét vé. Chỉ xác nhận vào cửa khi máy chủ đã ghi nhận.</p></header>@if(error()){<button class="secondary" [disabled]="busy()" (click)="load()">Thử lại</button>}@if(error()){<p class="notice error" role="alert">{{error()}}</p>}<div class="checkin-events">@for(a of events();track a.event.id){<article class="ticket-row"><etp-event-image [title]="a.event.name"/><div class="checkin-event-copy"><h2>{{a.event.name}}</h2><span class="status-badge">{{label(a.event.status)}}</span><p>{{a.event.venueName}}</p><p>Check-in: {{date(a.opensAt,a.event.timezone)}} – {{date(a.closesAt,a.event.timezone)}}</p><p>{{a.canCheckIn?'Đang nhận khách':'Chưa thể check-in trong thời điểm này'}}</p></div><a class="button-link" [routerLink]="['/check-in/events',a.event.id,'scan']">Mở soát vé</a></article>}@empty{@if(!busy()&&!error()){<section class="empty-state"><h2>Chưa có sự kiện được cấp quyền</h2><p>Liên hệ chủ tổ chức để được phân công. Tổ chức cần đang hoạt động.</p></section>}} </div>`
})
export class CheckinEventsComponent {
 private readonly api=inject(TicketApi);private readonly destroy=inject(DestroyRef);readonly events=signal<EventAccess[]>([]);readonly error=signal('');readonly busy=signal(false);readonly date=dateTime;readonly label=label;
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy());this.load();}load(){if(this.busy())return;this.busy.set(true);this.error.set('');this.api.events().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>this.events.set(e),error:e=>{this.events.set([]);this.error.set(ticketError(e));}});}
}
