import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { IconComponent } from '@event-ticketing/ui';
import { BookingApi, bookingError } from './booking-api';
import { CheckoutSession, Order, countdown, remainingSeconds } from './booking-models';
import { money } from '../../events/src/event-models';

@Component({
    selector: 'etp-payment-checkout',
    imports: [FormsModule, RouterLink, IconComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './payment-checkout.component.html', styleUrl: './payment-checkout.component.scss'
})
export class PaymentCheckoutComponent {
 private readonly api=inject(BookingApi);
 private readonly router=inject(Router);
 private readonly id=inject(ActivatedRoute).snapshot.paramMap.get('attemptId')!;
 readonly session=signal<CheckoutSession|null>(null);
 readonly order=signal<Order|null>(null);
 readonly busy=signal(false);
 readonly loading=signal(false);
 readonly error=signal('');
 readonly remaining=signal(0);
 readonly money=money;
 readonly countdown=countdown;
 method: 'QR'|'CARD'='QR';
 cardNumber='';
 submitted=false;
 private loadedAt=0;
 private destroyed=false;
 private poll?:ReturnType<typeof setTimeout>;

 constructor() {
  const timer=setInterval(()=>this.tick(),250);
  inject(DestroyRef).onDestroy(()=>{this.destroyed=true;this.cardNumber='';clearInterval(timer);clearTimeout(this.poll);});
  void this.load();
 }
 get validCard():boolean {return /^[0-9]{16}$/.test(this.cardNumber.replace(/ /g,''));}
 private tick():void {
  const s=this.session();
  if(s)this.remaining.set(remainingSeconds(s.attempt.methodExpiresAt??s.attempt.expiresAt,s.serverTime,performance.now()-this.loadedAt));
 }
 private async accept(s:CheckoutSession):Promise<void> {
  if(this.destroyed)return;
  this.session.set(s);this.loadedAt=performance.now();this.tick();
  if(s.attempt.status!=='PENDING')await this.router.navigate(['/payment/result',s.attempt.orderId],{replaceUrl:true});
 }
 private schedule():void {
  clearTimeout(this.poll);
  const a=this.session()?.attempt;
  if(!this.destroyed&&a?.status==='PENDING'&&(a.paymentMethod||a.scenario))
   this.poll=setTimeout(()=>void this.load(),1000);
 }
 async load():Promise<void> {
  if(this.loading()||this.busy()||this.destroyed)return;
  this.loading.set(true);this.error.set('');
  try {
   const s=await firstValueFrom(this.api.paymentSession(this.id));
   await this.accept(s);
   if(!this.order()&&!this.destroyed)this.order.set(await firstValueFrom(this.api.order(s.attempt.orderId)));
  } catch(e) {if(!this.destroyed)this.error.set(bookingError(e));}
  finally {this.loading.set(false);this.schedule();}
 }
 async submit():Promise<void> {
  this.submitted=true;
  if(this.busy()||this.loading()||this.remaining()<=0||(this.method==='CARD'&&!this.validCard))return;
  this.busy.set(true);this.error.set('');clearTimeout(this.poll);
  try {
   const s=await firstValueFrom(this.api.selectPaymentMethod(this.id,this.method,this.method==='CARD'?this.cardNumber.replace(/ /g,''):undefined));
   this.cardNumber='';await this.accept(s);
  } catch(e) {this.error.set(bookingError(e));}
  finally {this.busy.set(false);this.schedule();}
 }
}
