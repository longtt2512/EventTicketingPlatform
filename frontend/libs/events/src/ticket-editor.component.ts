import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Input, Output, ViewChild, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { FORM_FEEDBACK, ConfirmDialogComponent, ConfirmationState } from '@event-ticketing/ui';
import { EventApi, eventError } from './event-api';
import { EventDetail, TicketType, label, money, dateTime, localTime, utcTime } from './event-models';
@Component({
    selector: 'etp-ticket-editor', imports: [FormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './ticket-editor.component.html', styleUrl: './ticket-editor.component.scss'
})
export class TicketEditorComponent {
 @Input({required:true}) event!:EventDetail;@Input() readonly=false;@Output() prepareDetails=new EventEmitter<void>();@Output() changed=new EventEmitter<EventDetail>();@ViewChild('ticketForm') form?:NgForm;
 private readonly api=inject(EventApi);private readonly destroy=inject(DestroyRef);
 readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly editing=signal(false);readonly confirmation=new ConfirmationState();readonly label=label;readonly money=money;readonly date=dateTime;
 selected:TicketType|null=null;name='';description='';price=0;quota=1;saleStart='';saleEnd='';maxPerOrder=1;
 clearFeedback(){this.error.set('');this.message.set('');for(const control of Object.values(this.form?.controls??{}))if(control.hasError('eventRule'))control.updateValueAndValidity();}
 scheduleReady(){return !!this.event.capacity&&!!this.event.startAt&&Date.parse(this.event.startAt)>Date.now();}
 hasUnsavedChanges(){return this.editing()&&!!this.form?.dirty;}
 editable(){return !this.readonly&&['DRAFT','REJECTED','APPROVED','PUBLISHED'].includes(this.event.status);}
 total(){return this.event.ticketTypes.reduce((sum,t)=>sum+t.quota,0);}
 edit(ticket:TicketType|null){
  if(this.hasUnsavedChanges()&&!window.confirm('Bỏ các thay đổi hạng vé chưa lưu?'))return;
  this.selected=ticket;this.name=ticket?.name??'';this.description=ticket?.description??'';this.price=ticket?.price??0;this.quota=ticket?.quota??1;this.maxPerOrder=ticket?.maxPerOrder??1;
  this.saleStart=localTime(ticket?.saleStart??new Date().toISOString(),this.event.timezone);this.saleEnd=localTime(ticket?.saleEnd??this.event.startAt,this.event.timezone);this.editing.set(true);this.clearFeedback();this.form?.resetForm({name:this.name,description:this.description,price:this.price,quota:this.quota,saleStart:this.saleStart,saleEnd:this.saleEnd,maxPerOrder:this.maxPerOrder});
 }
 cancel(){if(this.hasUnsavedChanges()&&!window.confirm('Bỏ các thay đổi hạng vé chưa lưu?'))return;this.editing.set(false);this.clearFeedback();}
 save(){
  if(this.busy())return;this.clearFeedback();if(!this.form?.valid)return;
  const invalid=(field:string,message:string)=>{this.form?.controls[field]?.setErrors({eventRule:message});this.form?.controls[field]?.markAsTouched();this.error.set(message);document.getElementById(({quota:'ticket-quota',maxPerOrder:'ticket-max',saleStart:'sale-start',saleEnd:'sale-end',price:'ticket-price'} as Record<string,string>)[field])?.focus();};
  if(!Number.isSafeInteger(this.price)||this.price<0){invalid('price','Giá vé phải là số tiền VNĐ nguyên, không âm.');return;}
  if(!Number.isInteger(this.quota)||this.quota<1){invalid('quota','Số lượng vé phải là số nguyên từ 1 trở lên.');return;}
  if(this.quota<(this.selected?.sold??0)+(this.selected?.reserved??0)){invalid('quota','Số lượng vé không được thấp hơn tổng số vé đã bán và đang giữ.');return;}
  if(this.total()-(this.selected?.quota??0)+this.quota>(this.event.capacity??0)){invalid('quota',`Tổng số lượng vé vượt sức chứa ${this.event.capacity??0} chỗ. Hãy giảm số lượng vé hoặc tăng sức chứa ở bước Địa điểm.`);return;}
  if(!Number.isInteger(this.maxPerOrder)||this.maxPerOrder<1||this.maxPerOrder>this.quota){invalid('maxPerOrder','Số vé tối đa mỗi đơn phải từ 1 đến số lượng vé của hạng này.');return;}
  let saleStart:string,saleEnd:string;
  try{saleStart=utcTime(this.saleStart,this.event.timezone)!;saleEnd=utcTime(this.saleEnd,this.event.timezone)!;}catch{invalid('saleStart','Chọn ngày và giờ bán vé hợp lệ trong múi giờ sự kiện.');return;}
  if(!saleStart||!saleEnd||saleStart>=saleEnd){invalid('saleEnd','Thời gian đóng bán phải sau thời gian mở bán.');return;}
  if(!this.event.startAt||Date.parse(saleEnd)>Date.parse(this.event.startAt)){invalid('saleEnd','Thời gian đóng bán không được sau giờ bắt đầu sự kiện.');return;}
  const data={name:this.name.trim(),description:this.description,price:this.price,quota:this.quota,maxPerOrder:this.maxPerOrder,saleStart,saleEnd};
  const submit=()=>{this.busy.set(true);this.clearFeedback();this.api.ticket(this.event.organizationId,this.event.id,this.selected?.version??this.event.version,data,this.selected?.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>{this.form?.resetForm();this.editing.set(false);this.changed.emit(e);this.message.set('Đã lưu hạng vé.');},error:e=>this.error.set(eventError(e))});};
  if(this.event.status==='PUBLISHED')this.confirmation.ask({title:'Cập nhật hạng vé đang bán?',subject:this.name,description:`Giá mới ${money(this.price)}, số lượng ${this.quota} vé. Các giữ chỗ và giao dịch trước đó giữ nguyên giá.`,confirmLabel:'Cập nhật hạng vé'},submit);else submit();
 }
 pause(ticket:TicketType){
  this.clearFeedback();const pause=!ticket.paused;this.confirmation.ask({title:pause?'Tạm dừng bán hạng vé?':'Tiếp tục bán hạng vé?',subject:ticket.name,description:pause?'Ngừng nhận giữ chỗ mới. Các giữ chỗ hợp lệ hiện có vẫn được giữ.':'Vé chỉ mở bán trong khoảng thời gian đã thiết lập.',confirmLabel:pause?'Tạm dừng bán':'Tiếp tục bán'},()=>{
   this.busy.set(true);this.clearFeedback();this.api.pause(this.event.organizationId,this.event.id,ticket.id,ticket.version,pause).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>{this.changed.emit(e);this.message.set(pause?'Đã tạm dừng bán.':'Đã tiếp tục bán.');},error:e=>this.error.set(eventError(e))});
  });
 }
}
