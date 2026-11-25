import { refreshOnReturn } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ConfirmDialogComponent, ConfirmationState, UnsavedChangesDirective } from '@event-ticketing/ui';
import { EventApi,eventError } from './event-api';
import { EventDetail,label,dateTime } from './event-models';
import { EventImageComponent } from './event-image.component';
import { TicketEditorComponent } from './ticket-editor.component';
@Component({
    selector: 'etp-admin-events', imports: [FormsModule, ConfirmDialogComponent, UnsavedChangesDirective, EventImageComponent, TicketEditorComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './admin-events.component.html'
})
export class AdminEventsComponent {
 private readonly api=inject(EventApi);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
 readonly rows=signal<EventDetail[]>([]);readonly next=signal<string|null>(null);readonly selected=signal<EventDetail|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=label;readonly date=dateTime;readonly confirmation=new ConfirmationState();status='PENDING_REVIEW';reason='';
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.status!==(this.route.snapshot.queryParamMap.get('status')??'PENDING_REVIEW')||this.hasUnsavedChanges()||!!this.confirmation.current());this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(p=>{this.status=p.get('status')??'PENDING_REVIEW';this.load(p.get('event'));});}
 hasUnsavedChanges(){return !!this.reason.trim();}
 filter(){if(this.hasUnsavedChanges()&&!window.confirm('Bỏ ghi chú chưa gửi?'))return;this.reason='';void this.router.navigate([],{relativeTo:this.route,queryParams:{status:this.status}});}
 load(id:string|null=this.selected()?.id??null){this.busy.set(true);this.error.set('');this.message.set('');this.api.queue(this.status).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:page=>{this.rows.set(page.data);this.next.set(page.nextCursor);if(id)this.open(id,false);else this.selected.set(null);},error:e=>this.error.set(eventError(e))});}
 more(){const cursor=this.next();if(!cursor||this.busy())return;this.busy.set(true);this.api.queue(this.status,cursor).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:p=>{this.rows.update(rows=>[...rows,...p.data]);this.next.set(p.nextCursor);},error:e=>this.error.set(eventError(e))});}
 open(id:string,navigate=true){if(this.hasUnsavedChanges()&&!window.confirm('Bỏ ghi chú chưa gửi?'))return;this.reason='';if(navigate){void this.router.navigate([],{relativeTo:this.route,queryParams:{status:this.status,event:id}});return;}this.api.review(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:e=>this.selected.set(e),error:e=>this.error.set(eventError(e))});}
 decide(action:'approve'|'reject'|'cancel'|'feature') {
  const e=this.selected();if(!e||this.busy())return;
  if(['reject','cancel'].includes(action)&&!this.reason.trim()){this.error.set('Nhập lý do trước khi xác nhận.');document.getElementById('review-reason')?.focus();return;}
  const title={approve:'Phê duyệt sự kiện?',reject:'Từ chối sự kiện?',cancel:'Hủy sự kiện?',feature:e.featured?'Bỏ sự kiện nổi bật?':'Đánh dấu sự kiện nổi bật?'};
  const confirmLabel={approve:'Phê duyệt sự kiện',reject:'Từ chối sự kiện',cancel:'Hủy sự kiện',feature:e.featured?'Bỏ nổi bật':'Đặt nổi bật'};
  const reason=this.reason;
  this.confirmation.ask({title:title[action],subject:e.name,description:action==='approve'?'Phê duyệt đúng phiên bản đang xem. Nhà tổ chức sẽ thực hiện công khai sau đó.':action==='reject'?'Nhà tổ chức nhận được lý do để chỉnh sửa và gửi lại.':action==='cancel'?`Ngừng bán vé. Giữ lịch sử của ${e.ticketTypes.reduce((s,t)=>s+t.sold,0)} vé đã xác nhận. Không hoàn tiền tự động.`:'Thay đổi cách sự kiện xuất hiện trong danh sách nổi bật.',confirmLabel:confirmLabel[action],reason:reason||undefined,danger:['cancel','reject'].includes(action)},()=>{
   this.busy.set(true);this.error.set('');const request=action==='feature'?this.api.feature(e.id,e.version,!e.featured):this.api.moderate(e.id,e.version,action,reason);
   request.pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:value=>{this.selected.set(value);this.rows.update(rows=>rows.map(row=>row.id===value.id?value:row));this.reason='';this.message.set(action==='approve'?'Đã phê duyệt sự kiện.':action==='reject'?'Đã từ chối sự kiện.':action==='cancel'?'Đã hủy sự kiện.':'Đã cập nhật sự kiện nổi bật.');},error:error=>this.error.set(eventError(error))});
  });
 }
}
