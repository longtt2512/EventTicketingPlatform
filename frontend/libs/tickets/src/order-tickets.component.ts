import { refreshOnReturn } from '@event-ticketing/ui';
import {ChangeDetectionStrategy,Component,DestroyRef,Input,OnInit,inject,signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {TicketApi,ticketError} from './ticket-api';
import {IconComponent} from '@event-ticketing/ui';
import {Progress} from './ticket-models';
@Component({
    selector: 'etp-order-tickets', imports: [RouterLink, IconComponent], changeDetection: ChangeDetectionStrategy.OnPush, template: `<section class="order-fulfillment"><h2><etp-icon name="ticket" tone="indigo"/>Vé điện tử</h2>@if(error()){<p class="notice error" role="alert">{{error()}}</p>}@if(progress();as p){@if(p.status==='READY'){<p>Đã phát hành {{p.issuedCount}} / {{p.expectedCount}} vé.</p><a class="button-link" routerLink="/account/tickets" [queryParams]="{orderId:orderId}">Xem vé của đơn hàng</a>}@else if(p.status==='CANCELLED'){<p>Sự kiện đã hủy. Vé không còn quyền vào cửa.</p><a class="button-link secondary" routerLink="/account/tickets" [queryParams]="{orderId:orderId}">Xem trạng thái vé</a>}@else{<p role="status">Đang phát hành vé: {{p.issuedCount}} / {{p.expectedCount}}. Không cần tạo đơn mới.</p>}}@else if(busy()){<p role="status">Đang kiểm tra tiến trình phát hành…</p>}@if(error()){<button class="quiet" [disabled]="busy()" (click)="load()">Thử lại</button>}</section>`
})
export class OrderTicketsComponent implements OnInit {
 @Input({required:true})orderId='';private readonly api=inject(TicketApi);private readonly destroy=inject(DestroyRef);readonly progress=signal<Progress|null>(null);readonly error=signal('');readonly busy=signal(false);private timer?:ReturnType<typeof setInterval>;
 constructor(){refreshOnReturn(()=>this.load(),()=>this.busy());this.destroy.onDestroy(()=>clearInterval(this.timer));}
 ngOnInit(){this.load();this.timer=setInterval(()=>{if(this.progress()?.status!=='READY'&&this.progress()?.status!=='CANCELLED')this.load();},5000);}
 load(){if(this.busy())return;this.busy.set(true);this.error.set('');this.api.progress(this.orderId).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:p=>this.progress.set(p),error:e=>this.error.set(ticketError(e))});}
}
