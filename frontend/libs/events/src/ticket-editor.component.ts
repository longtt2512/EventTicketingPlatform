import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Input, Output, ViewChild, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { FORM_FEEDBACK, ConfirmDialogComponent, ConfirmationState } from '@event-ticketing/ui';
import { EventApi, eventError } from './event-api';
import { EventDetail, TicketType, label, money, dateTime, localTime, utcTime } from './event-models';
@Component({
    selector: 'etp-ticket-editor', imports: [FormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './ticket-editor.component.html'
})
export class TicketEditorComponent {
 @Input({required:true}) event!:EventDetail;@Input() readonly=false;@Output() changed=new EventEmitter<EventDetail>();@ViewChild('ticketForm') form?:NgForm;
 private readonly api=inject(EventApi);private readonly destroy=inject(DestroyRef);
 readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly editing=signal(false);readonly confirmation=new ConfirmationState();readonly label=label;readonly money=money;readonly date=dateTime;
 selected:TicketType|null=null;name='';description='';price=0;quota=1;saleStart='';saleEnd='';maxPerOrder=1;
 hasUnsavedChanges(){return this.editing()&&!!this.form?.dirty;}
 editable(){return !this.readonly&&['DRAFT','REJECTED','APPROVED','PUBLISHED'].includes(this.event.status);}
 total(){return this.event.ticketTypes.reduce((sum,t)=>sum+t.quota,0);}
 edit(ticket:TicketType|null){
  if(this.hasUnsavedChanges()&&!window.confirm('Bỏ các thay đổi hạng vé chưa lưu?'))return;
  this.selected=ticket;this.name=ticket?.name??'';this.description=ticket?.description??'';this.price=ticket?.price??0;this.quota=ticket?.quota??1;this.maxPerOrder=ticket?.maxPerOrder??1;
  this.saleStart=localTime(ticket?.saleStart??new Date().toISOString(),this.event.timezone);this.saleEnd=localTime(ticket?.saleEnd??this.event.startAt,this.event.timezone);this.editing.set(true);this.error.set('');this.form?.resetForm({name:this.name,description:this.description,price:this.price,quota:this.quota,saleStart:this.saleStart,saleEnd:this.saleEnd,maxPerOrder:this.maxPerOrder});
 }
 cancel(){if(this.hasUnsavedChanges()&&!window.confirm('Bỏ các thay đổi hạng vé chưa lưu?'))return;this.editing.set(false);}
 save(){
  if(this.busy()||!this.form?.valid)return;
  if(!Number.isInteger(this.price)||!Number.isInteger(this.quota)||!Number.isInteger(this.maxPerOrder)){this.error.set('Giá, hạn mức và số vé mỗi đơn phải là số nguyên.');return;}
  const data={name:this.name,description:this.description,price:this.price,quota:this.quota,maxPerOrder:this.maxPerOrder,saleStart:utcTime(this.saleStart,this.event.timezone)!,saleEnd:utcTime(this.saleEnd,this.event.timezone)!};
  const submit=()=>{this.busy.set(true);this.error.set('');this.api.ticket(this.event.organizationId,this.event.id,this.selected?.version??this.event.version,data,this.selected?.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>{this.form?.resetForm();this.editing.set(false);this.changed.emit(e);this.message.set('Đã lưu hạng vé.');},error:e=>this.error.set(eventError(e))});};
  if(this.event.status==='PUBLISHED')this.confirmation.ask({title:'Cập nhật hạng vé đang bán?',subject:this.name,description:`Giá mới ${money(this.price)}, hạn mức ${this.quota} vé. Các giữ chỗ và giao dịch trước đó giữ nguyên giá.`,confirmLabel:'Cập nhật hạng vé'},submit);else submit();
 }
 pause(ticket:TicketType){
  const pause=!ticket.paused;this.confirmation.ask({title:pause?'Tạm dừng bán hạng vé?':'Tiếp tục bán hạng vé?',subject:ticket.name,description:pause?'Ngừng nhận giữ chỗ mới. Các giữ chỗ hợp lệ hiện có vẫn được giữ.':'Vé chỉ mở bán trong khoảng thời gian đã thiết lập.',confirmLabel:pause?'Tạm dừng bán':'Tiếp tục bán'},()=>{
   this.busy.set(true);this.error.set('');this.api.pause(this.event.organizationId,this.event.id,ticket.id,ticket.version,pause).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>{this.changed.emit(e);this.message.set(pause?'Đã tạm dừng bán.':'Đã tiếp tục bán.');},error:e=>this.error.set(eventError(e))});
  });
 }
}
