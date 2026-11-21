import { ChangeDetectionStrategy,Component,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { FORM_FEEDBACK,UnsavedChangesDirective,ConfirmDialogComponent,ConfirmationState } from '@event-ticketing/ui';
import { BookingApi,bookingError } from './booking-api';
import { AdminAttempt,PaymentCase,orderLabel,currencyAmount } from './booking-models';
import { money,dateTime,localTime,utcTime } from '../../events/src/event-models';
@Component({
    selector: 'etp-admin-payments', imports: [FormsModule, RouterLink, ...FORM_FEEDBACK, UnsavedChangesDirective, ConfirmDialogComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './admin-payments.component.html'
})
export class AdminPaymentsComponent {
 private readonly api=inject(BookingApi);readonly session=inject(SessionStore);private readonly route=inject(ActivatedRoute);readonly exceptions=!!this.route.snapshot.data['exceptions'];
 readonly payments=signal<AdminAttempt[]>([]);readonly cases=signal<PaymentCase[]>([]);readonly selected=signal<PaymentCase|null>(null);readonly next=signal<string|null>(null);readonly busy=signal(false);readonly loading=signal(false);readonly error=signal('');readonly notice=signal('');readonly confirmation=new ConfirmationState();readonly money=money;readonly amountLabel=currencyAmount;readonly date=dateTime;readonly label=orderLabel;
 status='';orderId='';reason='';reference='';amount=0;evidence='';returnedAt='';dirty=false;
 constructor(){void this.load();}
 hasUnsavedChanges(){return this.dirty;}
 async load(more=false){if(this.loading())return;this.loading.set(true);this.error.set('');this.notice.set('');try{if(this.orderId&&!/^[a-f0-9-]{36}$/i.test(this.orderId))throw new Error('Nhập mã đơn hàng hợp lệ hoặc bỏ trống để xem tất cả.');const cursor=more?this.next()??undefined:undefined;if(this.exceptions){const p=await firstValueFrom(this.api.cases(this.status,this.orderId,cursor));this.cases.set(more?[...this.cases(),...p.data]:p.data);this.next.set(p.nextCursor);}else{const p=await firstValueFrom(this.api.payments(this.status,this.orderId,cursor));this.payments.set(more?[...this.payments(),...p.data]:p.data);this.next.set(p.nextCursor);}}catch(e){this.error.set(e instanceof Error?e.message:bookingError(e));}finally{this.loading.set(false);}}
 async open(id:string){if(this.dirty&&!window.confirm('Bạn có thông tin chưa lưu. Bỏ thay đổi để mở ngoại lệ khác?'))return;try{const c=await firstValueFrom(this.api.case(id));this.selected.set(c);this.reason='';this.reference='';this.amount=c.received_amount-c.returned_amount;this.evidence='';this.returnedAt=localTime(new Date().toISOString());this.dirty=false;}catch(e){this.error.set(bookingError(e));}}
 review(){const c=this.selected();if(!c||this.busy())return;const reason=this.reason.trim();this.confirmation.ask({title:'Nhận xử lý ngoại lệ?',subject:c.id,description:'Bạn sẽ phụ trách kiểm tra và ghi nhận bằng chứng cho giao dịch này.',confirmLabel:'Nhận xử lý',reason},()=>void this.run(()=>firstValueFrom(this.api.review(c.id,reason))));}
 resolve(){const c=this.selected();if(!c||this.busy())return;let returnedAt:string|null;try{returnedAt=utcTime(this.returnedAt);}catch(e){this.error.set(bookingError(e));return;}if(!returnedAt)return;const input={reference:this.reference.trim(),amount:this.amount,currency:c.currency,evidence:this.evidence.trim(),returnedAt};this.confirmation.ask({title:'Ghi nhận khoản hoàn?',subject:`${currencyAmount(input.amount,input.currency)} · ${input.reference}`,description:'Ghi nhận bằng chứng hoàn tiền đã kiểm tra. Khi hoàn đủ số tiền còn lại, ngoại lệ được đánh dấu đã xử lý.',confirmLabel:'Ghi nhận khoản hoàn',reason:input.evidence},()=>void this.run(()=>firstValueFrom(this.api.resolve(c.id,input))));}
 private async run(action:()=>Promise<PaymentCase>){this.busy.set(true);this.error.set('');try{const c=await action();this.selected.set(c);this.dirty=false;this.notice.set('Đã lưu kết quả xử lý.');this.cases.update(list=>list.map(item=>item.id===c.id?c:item));this.amount=c.received_amount-c.returned_amount;this.reference='';this.evidence='';}catch(e){this.error.set(bookingError(e));}finally{this.busy.set(false);}}
 reconcile(){this.confirmation.ask({title:'Chạy đối soát thanh toán?',subject:'Các giao dịch đang chờ',description:'Tra cứu lại tối đa 100 giao dịch. Tác vụ chỉ xác minh kết quả, không tạo lần thanh toán mới.',confirmLabel:'Chạy đối soát'},()=>void this.runReconcile());}
 private async runReconcile(){this.busy.set(true);try{const r=await firstValueFrom(this.api.reconcile());this.notice.set(`Đã đối soát ${r.examined} giao dịch.`);}catch(e){this.error.set(bookingError(e));}finally{this.busy.set(false);}}
}
