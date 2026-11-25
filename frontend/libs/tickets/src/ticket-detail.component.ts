import { refreshOnReturn } from '@event-ticketing/ui';
import { EventImageComponent } from '../../events/src/event-image.component';
import {ChangeDetectionStrategy,Component,DestroyRef,inject,signal} from '@angular/core';
import {ActivatedRoute,RouterLink} from '@angular/router';
import {firstValueFrom} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TicketApi,ticketError} from './ticket-api';
import {Ticket,ticketLabel} from './ticket-models';
import {dateTime} from '../../events/src/event-models';
@Component({
    selector: 'etp-ticket-detail', imports: [RouterLink, EventImageComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './ticket-detail.component.html'
})
export class TicketDetailComponent {
 private readonly api=inject(TicketApi);private readonly destroy=inject(DestroyRef);readonly id=inject(ActivatedRoute).snapshot.paramMap.get('ticketId')!;
 readonly ticket=signal<Ticket|null>(null);readonly busy=signal(false);readonly downloading=signal(false);readonly error=signal('');readonly pdfError=signal('');readonly date=dateTime;readonly label=ticketLabel;
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.downloading());void this.load();const timer=setInterval(()=>{if(document.visibilityState==='visible')void this.load();},15000);this.destroy.onDestroy(()=>clearInterval(timer));}
 async load(){if(this.busy())return;this.busy.set(true);this.error.set('');try{this.ticket.set(await firstValueFrom(this.api.ticket(this.id).pipe(takeUntilDestroyed(this.destroy))));}catch(e){this.error.set(ticketError(e));this.ticket.update(t=>t?{...t,qrImage:null}:null);}finally{this.busy.set(false);}}
 async download(){if(this.downloading())return;this.downloading.set(true);this.pdfError.set('');try{const blob=await firstValueFrom(this.api.pdf(this.id).pipe(takeUntilDestroyed(this.destroy)));const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`ticket-${this.id}.pdf`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);await this.load();}catch{this.pdfError.set('Chưa tải được PDF. Kiểm tra kết nối rồi thử lại. Vé của bạn vẫn được giữ nguyên.');}finally{this.downloading.set(false);}}
}
