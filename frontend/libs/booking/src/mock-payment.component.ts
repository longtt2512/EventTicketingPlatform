import { ChangeDetectionStrategy,Component,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,Router,RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { BookingApi,bookingError } from './booking-api';
import { Attempt } from './booking-models';
import { money,dateTime } from '../../events/src/event-models';
@Component({selector:'etp-mock-payment',standalone:true,imports:[FormsModule,RouterLink],changeDetection:ChangeDetectionStrategy.OnPush,templateUrl:'./mock-payment.component.html'})
export class MockPaymentComponent {
 private readonly api=inject(BookingApi);private readonly router=inject(Router);private readonly id=inject(ActivatedRoute).snapshot.paramMap.get('attemptId')!;
 readonly attempt=signal<Attempt|null>(null);readonly error=signal('');readonly busy=signal(false);readonly money=money;readonly date=dateTime;scenario='SUCCESS';
 readonly scenarios=[{id:'SUCCESS',name:'Thành công',description:'Gửi kết quả thành công để xác minh đơn.'},{id:'FAILED',name:'Thất bại',description:'Cho phép thử lại nếu đơn còn thời gian giữ vé.'},{id:'UNKNOWN',name:'Chưa rõ kết quả',description:'Giữ giao dịch đang chờ. Quay lại đây để chọn kết quả sau.'},{id:'DELAYED',name:'Phản hồi chậm',description:'Gửi kết quả thành công sau 15 giây.'},{id:'DUPLICATE',name:'Thông báo trùng',description:'Gửi cùng một giao dịch thành công hai lần.'},{id:'LATE_SUCCESS',name:'Thành công sau hạn',description:'Chờ hết thời gian giữ vé rồi gửi thành công, tạo ngoại lệ.'},{id:'WRONG_AMOUNT',name:'Số tiền không khớp',description:'Báo thu nhiều hơn 1.000 ₫. Đơn cần được đối soát.'},{id:'LOST_CALLBACK',name:'Mất thông báo',description:'Cổng thanh toán ghi nhận thành công. Tra cứu hoặc đối soát khôi phục kết quả.'}];
 constructor(){void this.load();}
 async load(){try{this.attempt.set(await firstValueFrom(this.api.mock(this.id)));}catch(e){this.error.set(bookingError(e));}}
 async submit(){if(this.busy())return;this.busy.set(true);this.error.set('');try{const a=await firstValueFrom(this.api.scenario(this.id,this.scenario));await this.router.navigate(['/payment/result',a.orderId]);}catch(e){this.error.set(bookingError(e));}finally{this.busy.set(false);}}
}
