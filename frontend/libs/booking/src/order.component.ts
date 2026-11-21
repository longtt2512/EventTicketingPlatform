import { refreshOnReturn } from '@event-ticketing/ui';
import { OrderTicketsComponent } from '@event-ticketing/tickets';
import { ChangeDetectionStrategy,Component,DestroyRef,computed,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,Router,RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { IconComponent,ConfirmDialogComponent,ConfirmationState } from '@event-ticketing/ui';
import { money,dateTime } from '../../events/src/event-models';
import { BookingApi,bookingError,requestKey,clearRequestKey } from './booking-api';
import { Order,PaymentStatus,remainingSeconds,countdown,orderLabel,currencyAmount } from './booking-models';
@Component({
    selector: 'etp-order', imports: [FormsModule, RouterLink, ConfirmDialogComponent, OrderTicketsComponent, IconComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './order.component.html'
})
export class OrderComponent {
 private readonly api=inject(BookingApi);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);readonly session=inject(SessionStore);
 readonly order=signal<Order|null>(null);readonly payments=signal<PaymentStatus>({attempts:[],exceptions:[]});readonly error=signal('');readonly paymentError=signal('');readonly busy=signal(false);readonly loading=signal(false);readonly remaining=signal(0);readonly confirmation=new ConfirmationState();readonly money=money;readonly amount=currencyAmount;readonly date=dateTime;readonly label=orderLabel;readonly countdown=countdown;
 readonly mode=this.route.snapshot.data['mode'] as 'checkout'|'result'|'detail';readonly readOnly=!!this.route.snapshot.data['readOnly'];readonly orderId=this.route.snapshot.paramMap.get('orderId')!;terms=false;
 private loadedAt=0;private expiredChecked=false;private destroyed=false;private polls=0;private poll?:ReturnType<typeof setTimeout>;
 readonly failureMessage=computed(()=>this.payments().attempts.find(a=>a.status==='FAILED')?.failureMessage);
 readonly title=computed(()=>this.mode==='checkout'?'Thanh toán':this.mode==='result'?'Kết quả đặt vé':'Chi tiết đơn hàng');
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.loading()||this.busy()||!!this.confirmation.current());const interval=setInterval(()=>this.tick(),1000);this.destroy.onDestroy(()=>{this.destroyed=true;clearInterval(interval);clearTimeout(this.poll);});void this.load();}
 async load(){if(this.loading()||this.destroyed)return;this.loading.set(true);this.error.set('');this.paymentError.set('');try{
   try{this.payments.set(await firstValueFrom(this.api.paymentStatus(this.orderId)));}catch(e){this.paymentError.set(bookingError(e));}
   this.order.set(await firstValueFrom(this.api.order(this.orderId)));this.loadedAt=performance.now();this.expiredChecked=false;this.tick();
  }catch(e){this.error.set(bookingError(e));}finally{this.loading.set(false);}
  const o=this.order();const waiting=o?.paymentStatus==='PENDING'||(this.payments().attempts.some(a=>a.status==='SUCCESS'&&a.result==='PENDING')&&o?.status==='PENDING_PAYMENT');
  if(waiting&&!this.destroyed){clearTimeout(this.poll);this.poll=setTimeout(()=>{if(document.visibilityState==='visible'&&navigator.onLine)void this.load();},this.polls++<30?1000:5000);}
 }
 private tick(){const o=this.order();if(!o)return;const seconds=remainingSeconds(o.expiresAt,o.serverTime,performance.now()-this.loadedAt);this.remaining.set(seconds);if(seconds===0&&o.status==='PENDING_PAYMENT'&&!this.expiredChecked){this.expiredChecked=true;if(!this.loading())void this.load();}}
 async pay(){const o=this.order();if(!o||this.busy()||!this.terms||this.remaining()<=0||!(o.canPay||o.canConfirmFree))return;this.busy.set(true);this.error.set('');const action=o.totalVnd===0?'free':'payment';const scope=`${action}:${o.id}`;
  try{if(o.totalVnd===0){this.order.set(await firstValueFrom(this.api.command(o.id,'confirm-free',requestKey(scope))));clearRequestKey(scope);await this.router.navigate(['/payment/result',o.id]);}else{const attempt=await firstValueFrom(this.api.startPayment(o.id,requestKey(scope)));clearRequestKey(scope);await this.router.navigate(['/payment/checkout',attempt.id]);}}
  catch(e){this.error.set(bookingError(e));}finally{this.busy.set(false);}
 }
 cancel(){const o=this.order();if(!o?.canCancel||this.busy())return;this.confirmation.ask({title:'Hủy đơn đang giữ vé?',subject:o.eventName,description:'Vé đang giữ sẽ được trả lại kho. Đơn này không thể khôi phục.',confirmLabel:'Hủy đơn hàng',danger:true},()=>void this.cancelConfirmed(o.id));}
 private async cancelConfirmed(id:string){this.busy.set(true);const scope=`cancel:${id}`;try{this.order.set(await firstValueFrom(this.api.command(id,'cancel',requestKey(scope))));clearRequestKey(scope);}catch(e){this.error.set(bookingError(e));}finally{this.busy.set(false);}}
}
