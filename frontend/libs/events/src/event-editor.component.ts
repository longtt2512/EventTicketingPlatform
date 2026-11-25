import { refreshOnReturn } from '@event-ticketing/ui';
import { activityLabel } from '../../ui/src/activity-labels';
import { ChangeDetectionStrategy, Component, DestroyRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { FORM_FEEDBACK, UnsavedChangesDirective, ConfirmDialogComponent, ConfirmationState } from '@event-ticketing/ui';
import { EventApi,eventError } from './event-api';
import { AttendeeRecord, CATEGORIES, EventDetail,EventInput,EventHistory,label,dateTime,localTime,utcTime } from './event-models';
import { EventDateDirective, eventDateError } from './event-date.directive';
import { EventImageComponent } from './event-image.component';
import { TicketEditorComponent } from './ticket-editor.component';
@Component({
    selector: 'etp-event-editor', imports: [FormsModule, RouterLink, EventDateDirective, ...FORM_FEEDBACK, UnsavedChangesDirective, ConfirmDialogComponent, EventImageComponent, TicketEditorComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './event-editor.component.html', styleUrl: './event-editor.component.scss'
})
export class EventEditorComponent {
 private readonly api=inject(EventApi);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
 readonly org=this.route.snapshot.paramMap.get('org')!;readonly id=this.route.snapshot.paramMap.get('event')!;
 readonly event=signal<EventDetail|null>(null);readonly busy=signal(false);readonly uploading=signal(false);readonly error=signal('');readonly message=signal('');readonly step=signal(1);readonly history=signal<EventHistory[]>([]);
 readonly attendees=signal<AttendeeRecord[]>([]);readonly attendeesLoaded=signal(false);readonly attendeeCursor=signal<string|null>(null);readonly attendeeBusy=signal(false);readonly confirmation=new ConfirmationState();readonly categories=signal<string[]>([]);readonly label=label;readonly actionLabel=activityLabel;readonly date=dateTime;
 @ViewChild('detailsForm') form?:NgForm;@ViewChild(TicketEditorComponent) tickets?:TicketEditorComponent;
 input:EventInput={name:'',category:null,description:'',venueName:'',venueAddress:'',city:'',timezone:'Asia/Ho_Chi_Minh',startAt:null,endAt:null,capacity:null,terms:'',bannerMediaId:null,galleryMediaIds:[]};start='';end='';private imageDirty=false;reason='';
 constructor(){const cancellationTimer=setInterval(()=>{if(document.visibilityState==='visible'&&this.event()?.cancellationPending&&!this.busy()&&!this.hasUnsavedChanges())this.load();},3000);this.destroy.onDestroy(()=>clearInterval(cancellationTimer));refreshOnReturn(()=>void this.load(),()=>this.busy()||this.uploading()||this.hasUnsavedChanges()||!!this.confirmation.current());this.api.categories().pipe(takeUntilDestroyed(this.destroy)).subscribe({next:items=>this.categories.set(items),error:()=>this.error.set('Chưa tải được danh mục. Tải lại trang để thử lại.')});this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(p=>this.step.set(Math.max(1,Math.min(4,Number(p.get('step'))||1))));this.load();}
 contentDirty(){return !!this.form?.dirty||this.imageDirty||this.uploading();}
 hasUnsavedChanges(){return !!this.form?.dirty||this.imageDirty||!!this.tickets?.hasUnsavedChanges()||!!this.reason.trim()||this.uploading();}
 editable(){return !this.event()?.cancellationPending&&['DRAFT','REJECTED','APPROVED'].includes(this.event()?.status??'');}
 clearFeedback(){this.error.set('');this.message.set('');}
 go(step:number){if(step===this.step())return;if(this.tickets?.hasUnsavedChanges()){this.clearFeedback();this.error.set('Lưu hạng vé hoặc đóng chỉnh sửa trước khi sang bước khác.');return;}this.tickets?.clearFeedback();if(this.contentDirty()&&this.editable()){this.save(step);return;}this.clearFeedback();this.navigate(step);}
 private navigate(step:number){void this.router.navigate([],{relativeTo:this.route,queryParams:{step},queryParamsHandling:'merge'});}
 openRequired(item:{step:number;field:string}){this.go(item.step);setTimeout(()=>document.getElementById(item.field)?.focus(),0);}
 earliestDate(){return localTime(new Date().toISOString(),this.input.timezone);}
 load(){if(this.hasUnsavedChanges()&&!window.confirm('Tải lại sẽ bỏ thay đổi chưa lưu. Tiếp tục?'))return;this.busy.set(true);this.error.set('');this.message.set('');this.api.detail(this.org,this.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>this.accept(e),error:e=>this.error.set(eventError(e))});}
 accept(e:EventDetail){
  this.event.set(e);this.input={name:e.name,category:e.category,description:e.description,venueName:e.venueName,venueAddress:e.venueAddress,city:e.city,timezone:e.timezone,startAt:e.startAt,endAt:e.endAt,capacity:e.capacity,terms:e.terms,bannerMediaId:e.bannerMediaId,galleryMediaIds:[...e.galleryMediaIds]};
  this.start=localTime(e.startAt,e.timezone);this.end=localTime(e.endAt,e.timezone);this.form?.form.markAsPristine();this.imageDirty=false;this.reason='';
  this.api.history(this.org,this.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:h=>this.history.set(h),error:()=>{}});
 }
 save(next?:number){
  if(this.busy()||this.uploading())return;this.clearFeedback();
  for(const part of ['start','end'] as const){const error=eventDateError(this[part],part,this.start,this.input.timezone);if(error){this.form?.controls[part]?.setErrors({eventRule:error});this.form?.controls[part]?.markAsTouched();this.error.set(error);this.navigate(2);setTimeout(()=>document.getElementById('event-'+part)?.focus(),0);return;}}
  if(!this.form?.valid){this.form?.form.markAllAsTouched();return;}let input:EventInput;try {input={...this.input,startAt:utcTime(this.start,this.input.timezone),endAt:utcTime(this.end,this.input.timezone)};}catch(e){this.error.set(eventError(e));return;}this.busy.set(true);
  this.api.update(this.org,this.id,this.event()!.version,input).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>{this.accept(e);this.message.set('Đã lưu bản nháp.');if(next){this.message.set('');this.navigate(next);}},error:e=>this.error.set(eventError(e))});
 }
 async upload(event:Event,kind:'BANNER'|'GALLERY'){
  const element=event.target as HTMLInputElement,file=element.files?.[0];if(!file||this.uploading())return;this.uploading.set(true);this.clearFeedback();
  try {const id=await this.api.upload(this.org,this.id,file,kind);if(kind==='BANNER')this.input.bannerMediaId=id;else this.input.galleryMediaIds=[...this.input.galleryMediaIds,id];this.imageDirty=true;this.message.set('Ảnh đã tải lên. Lưu bản nháp để gắn ảnh vào sự kiện.');}
  catch(e){this.error.set(eventError(e));}finally{this.uploading.set(false);element.value='';}
 }
 removeImage(id:string){this.clearFeedback();this.input.galleryMediaIds=this.input.galleryMediaIds.filter(x=>x!==id);this.imageDirty=true;}
 missing(){
  const e=this.event();if(!e)return [];
  const items:{label:string;step:number;field:string}[]=[];
  const add=(missing:boolean,label:string,step:number,field:string)=>{if(missing)items.push({label,step,field});};
  add(!e.name.trim(),'Tên sự kiện',1,'event-title');add(!e.category,'Danh mục',1,'event-category');add(!e.description.trim(),'Mô tả sự kiện',1,'event-description');add(!e.bannerMediaId,'Ảnh bìa',1,'banner-button');
  add(!e.venueName.trim(),'Tên địa điểm',2,'venue-name');add(!e.venueAddress.trim(),'Địa chỉ',2,'venue-address');add(!e.city.trim(),'Thành phố',2,'event-city');
  add(!e.startAt||Date.parse(e.startAt)<=Date.now(),'Thời gian bắt đầu trong tương lai',2,'event-start');
  add(!e.endAt||Date.parse(e.endAt)<=Date.now()||!!e.startAt&&Date.parse(e.endAt)<=Date.parse(e.startAt),'Thời gian kết thúc sau giờ bắt đầu',2,'event-end');
  add(!e.capacity,'Sức chứa tối đa',2,'event-capacity');add(!e.terms.trim(),'Điều khoản tham dự',2,'event-terms');add(!e.ticketTypes.length,'Ít nhất một hạng vé',3,'add-ticket');return items;
 }
 action(action:'submit'|'publish'|'cancel') {
  const e=this.event();if(!e||this.busy())return;this.clearFeedback();if(action==='submit'&&this.missing().length){this.error.set('Bổ sung các mục được đánh dấu bên dưới trước khi gửi kiểm duyệt.');return;}if(this.hasUnsavedChanges()&&action!=='cancel'){this.error.set('Lưu hoặc bỏ các thay đổi đang nhập trước khi tiếp tục.');return;}
  if(action==='cancel'&&!this.reason.trim()){this.error.set('Nhập lý do hủy sự kiện.');document.getElementById('cancel-reason')?.focus();return;}
  const titles={submit:'Gửi sự kiện để kiểm duyệt?',publish:'Công khai sự kiện?',cancel:'Hủy sự kiện?'};const labels={submit:'Gửi kiểm duyệt',publish:'Công khai sự kiện',cancel:'Hủy sự kiện'};
  const sold=e.ticketTypes.reduce((s,t)=>s+t.sold,0),reserved=e.ticketTypes.reduce((s,t)=>s+t.reserved,0);const reason=this.reason;
  this.confirmation.ask({title:titles[action],subject:e.name,description:action==='submit'?'Nội dung sẽ được khóa trong thời gian chờ duyệt.':action==='publish'?'Khách có thể tìm thấy sự kiện và xem các hạng vé. Nội dung chính sẽ được khóa.':`Ngừng bán vé và giải phóng ${reserved} vé đang giữ. Có ${sold} vé đã xác nhận. Lịch sử được giữ lại. Không hoàn tiền tự động.`,reason:action==='cancel'?reason:undefined,confirmLabel:labels[action],danger:action==='cancel'},()=>{
   this.busy.set(true);this.clearFeedback();this.api.command(this.org,this.id,e.version,action,reason).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.accept(r);this.message.set(action==='submit'?'Đã gửi sự kiện để kiểm duyệt.':action==='publish'?'Sự kiện đã được công khai.':'Đã hủy sự kiện.');},error:error=>this.error.set(eventError(error))});
  });
 }
 loadAttendees(more=false){if(this.attendeeBusy())return;this.clearFeedback();this.attendeeBusy.set(true);this.api.attendees(this.org,this.id,more?this.attendeeCursor()??undefined:undefined).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.attendeeBusy.set(false))).subscribe({next:r=>{this.attendees.set(more?[...this.attendees(),...r.data]:r.data);this.attendeeCursor.set(r.nextCursor);this.attendeesLoaded.set(true);},error:e=>this.error.set(eventError(e))});}
}
