import { ChangeDetectionStrategy,Component,computed,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,Router,RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { EventImageComponent } from '../../events/src/event-image.component';
import { IconComponent, FORM_FEEDBACK } from '@event-ticketing/ui';
import { money,dateTime,label } from '../../events/src/event-models';
import { BookingApi,bookingError,requestKey,clearRequestKey } from './booking-api';
import { BookingQuote,parseSelection } from './booking-models';
@Component({
    selector: 'etp-booking', imports: [FormsModule, RouterLink, EventImageComponent, IconComponent, ...FORM_FEEDBACK], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './booking.component.html'
})
export class BookingComponent {
 private readonly api=inject(BookingApi);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);readonly session=inject(SessionStore);
 readonly quote=signal<BookingQuote|null>(null);readonly quantities=signal<Partial<Record<string,number>>>(parseSelection(this.route.snapshot.queryParamMap.get('selection')));readonly loading=signal(false);readonly busy=signal(false);readonly error=signal('');readonly needsReview=signal(false);readonly money=money;readonly date=dateTime;readonly label=label;terms=false;
 readonly total=computed(()=>this.quote()?.event.ticketTypes.reduce((sum,t)=>sum+t.price*(this.quantities()[t.id]??0),0)??0);
 readonly count=computed(()=>Object.values(this.quantities()).reduce<number>((a,b)=>a+(b??0),0));
 private readonly eventId=this.route.snapshot.paramMap.get('eventId')!;
 constructor(){void this.load();}
 async load(){this.loading.set(true);this.error.set('');try{this.quote.set(await firstValueFrom(this.api.quote(this.eventId)));this.needsReview.set(false);}catch(e){this.error.set(bookingError(e));}finally{this.loading.set(false);}}
 set(id:string,value:unknown){const n=Number(value);this.quantities.update(q=>({...q,[id]:Number.isSafeInteger(n)&&n>=0?n:0}));this.terms=false;const selection=Object.entries(this.quantities()).filter(([,n])=>(n??0)>0).map(([id,n])=>`${id}:${n}`).join(',');void this.router.navigate([],{relativeTo:this.route,queryParams:{selection:selection||null},replaceUrl:true});}
 async reserve(){if(this.busy()||!this.quote())return;this.error.set('');const q=this.quote()!;const lines=Object.entries(this.quantities()).filter(([,n])=>(n??0)>0).map(([ticketTypeId,quantity])=>({ticketTypeId,quantity:quantity??0}));
  if(!lines.length){this.error.set('Hãy chọn ít nhất một vé để tiếp tục.');return;}
  if(lines.some(l=>{const t=q.event.ticketTypes.find(t=>t.id===l.ticketTypeId);return !t||t.saleState!=='ON_SALE'||l.quantity>Math.min(t.available,t.maxPerOrder);})){this.error.set('Số lượng đã chọn vượt giới hạn hoặc hạng vé không còn mở bán. Hãy điều chỉnh lựa chọn.');return;}
  if(!this.terms){this.error.set('Vui lòng đồng ý điều khoản tham dự.');return;}
  if(!this.session.isAuthenticated()){await this.router.navigate(['/login'],{queryParams:{returnUrl:this.router.url}});return;}
  this.busy.set(true);const scope=`reserve:${this.session.user()?.id}:${this.eventId}:${q.selectionVersion}:${JSON.stringify(lines)}`;
  try{const order=await firstValueFrom(this.api.reserve(this.eventId,q.selectionVersion,lines,requestKey(scope)));clearRequestKey(scope);await this.router.navigate(['/checkout',order.id]);}catch(e){this.error.set(bookingError(e));this.needsReview.set(true);}finally{this.busy.set(false);}
 }
}
