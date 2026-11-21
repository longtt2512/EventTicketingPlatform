import { refreshOnReturn } from '@event-ticketing/ui';
import { ChangeDetectionStrategy,Component,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { BookingApi,bookingError } from './booking-api';
import { Order,orderLabel } from './booking-models';
import { money,dateTime } from '../../events/src/event-models';
@Component({
    selector: 'etp-orders', imports: [FormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './orders.component.html'
})
export class OrdersComponent {private expanded=false;
 private readonly api=inject(BookingApi);private readonly route=inject(ActivatedRoute);readonly org=this.route.snapshot.paramMap.get('org')??undefined;readonly event=this.route.snapshot.paramMap.get('event')??undefined;
 readonly data=signal<Order[]>([]);readonly selected=signal<Order|null>(null);readonly error=signal('');readonly loading=signal(false);readonly next=signal<string|null>(null);readonly money=money;readonly date=dateTime;readonly label=orderLabel;status='';private appliedStatus='';
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.loading()||this.expanded||this.status!==this.appliedStatus);void this.load();}
 async load(more=false){if(this.loading())return;this.expanded=more;if(!more)this.appliedStatus=this.status;this.loading.set(true);this.error.set('');try{const p=await firstValueFrom(this.api.orders(this.appliedStatus,more?this.next()??undefined:undefined,this.org,this.event));this.data.set(more?[...this.data(),...p.data]:p.data);this.next.set(p.nextCursor);}catch(e){this.error.set(bookingError(e));}finally{this.loading.set(false);}}
 async detail(id:string){if(!this.org||!this.event)return;try{this.selected.set(await firstValueFrom(this.api.scopedOrder(this.org,this.event,id)));}catch(e){this.error.set(bookingError(e));}}
}
