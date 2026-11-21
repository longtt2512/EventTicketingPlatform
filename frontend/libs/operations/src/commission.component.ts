import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmDialogComponent, ConfirmationState, FORM_FEEDBACK, UnsavedChangesDirective } from '@event-ticketing/ui';
import { dateTime, utcTime } from '../../events/src/event-models';
import { OperationsApi, operationError } from './operations-api';
import { CommissionConfig,Configuration,CommissionChange,amount } from './operation-models';
@Component({
    selector: 'etp-commission', imports: [FormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './commission.component.html', styleUrl: './operations.scss'
})
export class CommissionComponent {
 readonly api=inject(OperationsApi);private readonly destroy=inject(DestroyRef);readonly config=signal<CommissionConfig|null>(null);readonly history=signal<Configuration[]>([]);readonly next=signal<string|null>(null);readonly busy=signal(false);readonly error=signal('');readonly notice=signal('');readonly confirmation=new ConfirmationState();readonly date=dateTime;readonly amount=amount;
 mode:'PERCENTAGE'|'FIXED_AMOUNT'='PERCENTAGE';value=5;effectiveAt='';reason='';dirty=false;
 constructor(){this.load();this.historyPage();}
 hasUnsavedChanges(){return this.dirty;}
 load(){this.busy.set(true);this.error.set('');this.api.config().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.config.set(r);if(!this.dirty){this.mode=r.current.mode;this.value=r.current.mode==='PERCENTAGE'?(r.current.rateBps??0)/100:r.current.fixedFeeVnd??0;}},error:e=>this.error.set(operationError(e))});}
 historyPage(more=false){this.api.configHistory(more?this.next()??undefined:undefined).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:p=>{this.history.set(more?[...this.history(),...p.data]:p.data);this.next.set(p.nextCursor==null?null:String(p.nextCursor));},error:e=>this.error.set(operationError(e))});}
 save(form?:NgForm){const c=this.config();if(!c||this.busy())return;let input:CommissionChange;try{if(!Number.isFinite(this.value)||this.value<0||(this.mode==='PERCENTAGE'&&(this.value>100||Math.abs(this.value*100-Math.round(this.value*100))>0.00001))||(this.mode==='FIXED_AMOUNT'&&!Number.isSafeInteger(this.value)))throw new Error();input={mode:this.mode,rateBps:this.mode==='PERCENTAGE'?Math.round(this.value*100):null,fixedFeeVnd:this.mode==='FIXED_AMOUNT'?this.value:null,effectiveAt:utcTime(this.effectiveAt),reason:this.reason.trim()};}catch{this.error.set('Phần trăm từ 0–100%, tối đa hai chữ số thập phân; phí cố định là số nguyên VND không âm.');return;}if(!input.reason)return;
 this.confirmation.ask({title:'Áp dụng cấu hình phí mới?',subject:input.mode==='PERCENTAGE'?this.value+'% mỗi vé':amount(this.value)+' mỗi vé',description:'Organizer chịu phí. Đơn đã tạo giữ nguyên snapshot; cấu hình mới chỉ áp dụng từ thời điểm hiệu lực.',reason:input.reason,confirmLabel:'Lưu cấu hình phí'},()=>{this.busy.set(true);this.error.set('');this.api.changeConfig(c.version,input).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.config.set(r);this.dirty=false;this.reason='';this.effectiveAt='';form?.form.markAsPristine();form?.form.markAsUntouched();this.notice.set('Đã lưu cấu hình và lịch sử thay đổi.');this.historyPage();},error:e=>this.error.set(operationError(e))});});}
}
