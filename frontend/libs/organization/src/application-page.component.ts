import { refreshOnReturn } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FORM_FEEDBACK, UnsavedChangesDirective, discardChangesMessage } from '@event-ticketing/ui';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, switchMap } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { Application, ApplicationInput, OrganizationApi, statusLabel, downloadBlob } from './organization-api';
@Component({
    selector: 'etp-organizer-application-page', imports: [FormsModule, ...FORM_FEEDBACK, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `<div [etpUnsavedChanges]="hasUnsavedChanges()"><header class="page-heading"><h1>Hồ sơ nhà tổ chức</h1><p>Cung cấp thông tin pháp lý để quản trị viên xác minh tổ chức của bạn.</p></header>
 @if(loading()){<p role="status">Đang tải hồ sơ…</p>} @else {
 @if(error()){<p class="notice error" role="alert">{{error()}}</p>} @if(message()){<p class="notice success" role="status">{{message()}}</p>}
 @if(application();as app){<section class="panel"><h2>Trạng thái hồ sơ</h2><p class="badge">{{label(app.status)}}</p>
 @if(app.rejectionReason){<p class="notice warning">Lý do từ chối: {{app.rejectionReason}}</p>}
 @if(app.status==='PENDING_VERIFICATION'){<p>Hồ sơ đang được xét duyệt. Bạn có thể sửa và gửi lại nếu hồ sơ bị từ chối.</p>}
 @if(app.status==='APPROVED'){<p>Tổ chức đã được xác minh.</p><a class="button-link" href="/org/organizations">Mở cổng nhà tổ chức</a>}
 @if(app.documentId){<button type="button" class="secondary" [disabled]="busy()" (click)="download()">Tải tài liệu đã gửi</button>}
 </section>}
 <section class="panel"><h2>Thông tin xác minh</h2><form etpForm #form="ngForm" (ngSubmit)="sendBack ? resubmit() : submit()">
 <fieldset [disabled]="busy()||readOnly()" style="border:0;padding:0;min-width:0">
 <label for="legal-name">Tên pháp lý tổ chức</label><input etpField id="legal-name" name="organizationName" [(ngModel)]="data.organizationName" required pattern=".*\\S.*" maxlength="200">
 <label for="contact-email">Email liên hệ</label><input etpField id="contact-email" name="contactEmail" type="email" [(ngModel)]="data.contactEmail" required email maxlength="320">
 <label for="address">Địa chỉ</label><textarea etpField id="address" name="address" [(ngModel)]="data.address" required maxlength="1000"></textarea>
 <label for="description">Giới thiệu tổ chức</label><textarea etpField id="description" name="description" [(ngModel)]="data.description" maxlength="2000"></textarea>
 @if(!readOnly()) {<label for="verification-file">Tài liệu xác minh</label><input id="verification-file" type="file" accept="application/pdf,image/png,image/jpeg" (change)="upload($event)"><p class="small">PDF, PNG hoặc JPEG, tối đa 5 MB. Chỉ bạn và quản trị viên xét duyệt được tải tài liệu.</p>
 @if(data.documentId){<p role="status">Tài liệu đã tải lên.</p>}}
 </fieldset>
 @if(!readOnly()){<div class="actions"><button type="submit" (click)="setSubmitMode(false)" [disabled]="busy()">{{application()?'Lưu chỉnh sửa':'Gửi hồ sơ'}}</button>
 @if(application()?.status==='REJECTED'){<button type="submit" [disabled]="busy()" (click)="setSubmitMode(true)">Lưu và gửi lại</button>}</div>}
 </form></section>@if(error()){<button class="secondary" [disabled]="busy()" (click)="load()">Thử lại</button>}}</div>`
})
export class OrganizerApplicationPageComponent {
 private readonly destroy=inject(DestroyRef);
 readonly api=inject(OrganizationApi);readonly application=signal<Application|null>(null);readonly loading=signal(true);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=statusLabel;
 data:ApplicationInput={organizationName:'',contactEmail:'',address:'',description:'',documentId:''};
 private baseline=JSON.stringify(this.data); sendBack=false;
 setSubmitMode(resubmit:boolean):void{this.sendBack=resubmit;}
 hasUnsavedChanges(){return !this.readOnly()&&JSON.stringify(this.data)!==this.baseline;}
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.loading()||this.busy()||this.hasUnsavedChanges());this.load();}
 readOnly(){return !!this.application()&&this.application()?.status!=='REJECTED';}
 load(){if(this.hasUnsavedChanges()&&!window.confirm(discardChangesMessage))return;this.loading.set(true);this.error.set('');this.api.mine().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading.set(false))).subscribe({next:a=>{this.application.set(a);this.data={organizationName:a.organizationName,contactEmail:a.contactEmail,address:a.address,description:a.description??'',documentId:a.documentId??''};this.baseline=JSON.stringify(this.data);},error:(e:HttpErrorResponse)=>{if(e.status!==404)this.error.set(errorMessage(e));}});}
 upload(event:Event){const file=(event.target as HTMLInputElement).files?.[0];if(!file)return;this.error.set('');if(file.size>5242880||!['application/pdf','image/png','image/jpeg'].includes(file.type)){this.error.set('Tài liệu phải là PDF, PNG hoặc JPEG và không quá 5 MB.');return;}this.busy.set(true);this.api.upload(file).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>this.data.documentId=r.id,error:e=>this.error.set(errorMessage(e))});}
 validateDocument(){if(this.data.documentId)return true;this.error.set('Hãy tải tài liệu PDF, PNG hoặc JPEG để gửi hồ sơ.');document.getElementById('verification-file')?.focus();return false;}
 submit(){if(this.busy()||this.readOnly()||!this.validateDocument())return;this.busy.set(true);this.error.set('');const app=this.application();(app?this.api.edit(app.id,this.data):this.api.apply(this.data)).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:a=>{this.application.set(a);this.baseline=JSON.stringify(this.data);this.message.set(app?'Đã lưu chỉnh sửa.':'Đã gửi hồ sơ để xét duyệt.');},error:e=>this.error.set(errorMessage(e))});}
 resubmit(){const app=this.application();if(!app||this.busy()||!this.validateDocument())return;this.busy.set(true);this.error.set('');this.api.edit(app.id,this.data).pipe(takeUntilDestroyed(this.destroy),switchMap(()=>this.api.resubmit(app.id)),finalize(()=>this.busy.set(false))).subscribe({next:a=>{this.application.set(a);this.baseline=JSON.stringify(this.data);this.message.set('Đã gửi lại hồ sơ.');},error:e=>this.error.set(errorMessage(e))});}
 download(){const id=this.application()?.documentId;if(!id)return;this.busy.set(true);this.api.document(id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:downloadBlob,error:e=>this.error.set(errorMessage(e))});}
}
