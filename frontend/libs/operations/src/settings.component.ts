import { refreshOnReturn } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmDialogComponent, ConfirmationState, FORM_FEEDBACK, UnsavedChangesDirective } from '@event-ticketing/ui';
import { OperationsApi, operationError } from './operations-api';
import { Settings,Setting } from './operation-models';
@Component({
    selector: 'etp-settings', imports: [FormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './settings.component.html', styleUrl: './operations.scss'
})
export class SettingsComponent {
 readonly api=inject(OperationsApi);private readonly destroy=inject(DestroyRef);readonly settings=signal<Settings|null>(null);readonly busy=signal(false);readonly error=signal('');readonly notice=signal('');readonly confirmation=new ConfirmationState();values:Record<string,string|number>={};reasons:Record<string,string>={};dirty=false;
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges()||!!this.confirmation.current());this.load();}hasUnsavedChanges(){return this.dirty;}
 label(key:string){return key==='reminderLeadHours'?'Nhắc sự kiện trước (giờ)':'Email hỗ trợ';}
 load(){this.busy.set(true);this.error.set('');this.api.settings().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.settings.set(r);if(!this.dirty)r.settings.forEach(s=>this.values[s.key]=s.value);},error:e=>this.error.set(operationError(e))});}
 save(s:Setting,form?:NgForm){if(this.busy())return;const value=this.values[s.key],reason=this.reasons[s.key]?.trim();if(!reason||reason.length<3)return;this.confirmation.ask({title:'Lưu cấu hình vận hành?',subject:this.label(s.key)+': '+value,description:'Thay đổi được ghi vào nhật ký. Thông báo nhắc đã gửi không được gửi lại.',reason,confirmLabel:'Lưu cấu hình'},()=>{this.busy.set(true);this.error.set('');this.api.saveSetting(s.key,s.version,value,reason).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.settings.set(r);this.reasons[s.key]='';form?.form.markAsPristine();form?.form.markAsUntouched();this.dirty=r.settings.some(row=>this.values[row.key]!==row.value||!!this.reasons[row.key]);this.notice.set('Đã lưu '+this.label(s.key).toLowerCase()+'.');},error:e=>this.error.set(operationError(e))});});}
}
