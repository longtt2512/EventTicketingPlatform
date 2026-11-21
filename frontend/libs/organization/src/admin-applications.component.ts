import { refreshOnReturn } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { ConfirmDialogComponent, ConfirmationState, UnsavedChangesDirective, discardChangesMessage } from '@event-ticketing/ui';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { Application, OrganizationApi, statusLabel, downloadBlob } from './organization-api';
@Component({
    selector: 'etp-admin-applications', imports: [FormsModule, ConfirmDialogComponent, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `<div [etpUnsavedChanges]="hasUnsavedChanges()"><header class="page-heading"><h1>Duyệt hồ sơ nhà tổ chức</h1><p>Kiểm tra thông tin và tài liệu trước khi quyết định.</p></header>
 @if(error()){<p class="notice error" role="alert">{{error()}}</p>} @if(message()){<p class="notice success" role="status">{{message()}}</p>}
 @if(error()){<button class="secondary" [disabled]="busy()" (click)="load()">Thử lại</button>}
 @if(busy()){<p role="status">Đang xử lý…</p>}
 <div class="application-review-layout"><section class="panel table-scroll"><table><thead><tr><th>Tổ chức</th><th>Trạng thái</th><th>Hành động</th></tr></thead><tbody>@for(a of applications();track a.id){<tr><td>{{a.organizationName}}</td><td>{{label(a.status)}}</td><td><button class="secondary" (click)="select(a)">Xem hồ sơ</button></td></tr>}@empty{<tr><td colspan="3">Chưa có hồ sơ.</td></tr>}</tbody></table></section>
 @if(selected();as a){<section class="panel"><h2>{{a.organizationName}}</h2><dl class="review-details"><dt>Email liên hệ</dt><dd>{{a.contactEmail}}</dd><dt>Địa chỉ</dt><dd>{{a.address}}</dd><dt>Giới thiệu</dt><dd>{{a.description}}</dd><dt>Trạng thái</dt><dd>{{label(a.status)}}</dd></dl>
 @if(a.rejectionReason){<p class="notice warning">{{a.rejectionReason}}</p>}
 @if(a.documentId){<button class="secondary" [disabled]="busy()" (click)="download(a.documentId)">Tải tài liệu xác minh</button>}@else{<p class="notice warning">Hồ sơ chưa có tài liệu. Từ chối để người nộp bổ sung.</p>}
 @if(a.status==='PENDING_VERIFICATION'){<div class="review-decision"><label for="decision-reason">Lý do từ chối</label><textarea id="decision-reason" [(ngModel)]="reason" maxlength="2000" aria-describedby="decision-help"></textarea><p class="small" id="decision-help">Bắt buộc có lý do khi từ chối; nội dung này được gửi cho người nộp.</p><div class="actions"><button [disabled]="busy()||!a.documentId||!a.address" (click)="decide(true)">Phê duyệt hồ sơ</button><button class="danger" [disabled]="busy()||!reason.trim()" (click)="decide(false)">Từ chối hồ sơ</button></div></div>}
 </section>}</div></div>@if(confirmation.current();as decision){<etp-confirm-dialog [confirmation]="decision" (resolved)="confirmation.resolve($event)"/>}`
})
export class AdminApplicationsPageComponent {
 private readonly destroy=inject(DestroyRef);
 readonly api=inject(OrganizationApi);readonly applications=signal<Application[]>([]);readonly selected=signal<Application|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=statusLabel;reason='';
 readonly confirmation=new ConfirmationState();
 hasUnsavedChanges(){return !!this.reason;}
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges()||!!this.confirmation.current());this.load();}
 load(){if(this.hasUnsavedChanges()&&!window.confirm(discardChangesMessage))return;this.busy.set(true);this.error.set('');this.api.applications().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:a=>{this.applications.set(a);const id=this.selected()?.id;if(id)this.selected.set(a.find(x=>x.id===id)??null);},error:e=>this.error.set(errorMessage(e))});}
 select(a:Application){if(this.hasUnsavedChanges()&&!window.confirm(discardChangesMessage))return;this.selected.set(a);this.reason='';this.message.set('');}
 decide(approve:boolean){
  const a=this.selected();if(!a||this.busy()||a.status!=='PENDING_VERIFICATION'||(!approve&&!this.reason.trim()))return;
  const reason=this.reason.trim();
  this.confirmation.ask({title:approve?'Phê duyệt hồ sơ?':'Từ chối hồ sơ?',subject:a.organizationName,description:approve?'Tổ chức được xác minh và có thể quản lý hoạt động theo quyền đã cấp.':'Người nộp sẽ nhận lý do để chỉnh sửa và gửi lại hồ sơ.',reason,confirmLabel:approve?'Phê duyệt hồ sơ':'Từ chối hồ sơ',danger:!approve},()=>{
   this.busy.set(true);this.error.set('');
   this.api.decide(a.id,approve,reason).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.reason='';this.message.set(approve?'Đã phê duyệt tổ chức.':'Đã từ chối hồ sơ và lưu lý do.');this.load();},error:e=>this.error.set(errorMessage(e))});
  });
 }

 download(id:string){this.busy.set(true);this.api.document(id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:downloadBlob,error:e=>this.error.set(errorMessage(e))});}
}
