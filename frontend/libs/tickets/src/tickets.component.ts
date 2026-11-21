import { refreshOnReturn } from '@event-ticketing/ui';
import { EventImageComponent } from '../../events/src/event-image.component';
import {ChangeDetectionStrategy,Component,DestroyRef,inject,signal} from '@angular/core';
import {ActivatedRoute,RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {TicketApi,ticketError} from './ticket-api';
import {Ticket,ticketLabel} from './ticket-models';
import {dateTime} from '../../events/src/event-models';
@Component({
    selector: 'etp-tickets', imports: [RouterLink, EventImageComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './tickets.component.html'
})
export class TicketsComponent {private expanded=false;
 readonly api=inject(TicketApi);private readonly destroy=inject(DestroyRef);readonly orderId=inject(ActivatedRoute).snapshot.queryParamMap.get('orderId')??undefined;
 readonly items=signal<Ticket[]>([]);readonly busy=signal(false);readonly error=signal('');readonly cursor=signal<string|null>(null);readonly label=ticketLabel;readonly date=dateTime;
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.expanded);this.load();}
 load(more=false){if(this.busy())return;this.expanded=more;this.busy.set(true);this.error.set('');this.api.tickets(this.orderId,more?this.cursor()??undefined:undefined).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:p=>{this.items.set(more?[...this.items(),...p.data]:p.data);this.cursor.set(p.nextCursor);},error:e=>this.error.set(ticketError(e))});}
}
