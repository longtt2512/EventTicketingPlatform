import { refreshOnReturn } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, ViewChild, Input, OnInit, inject, signal } from '@angular/core';
import { FORM_FEEDBACK, ConfirmDialogComponent, ConfirmationState } from '@event-ticketing/ui';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { Organization, Payout, OrganizationApi, statusLabel } from './organization-api';
@Component({
    selector: 'etp-payouts', imports: [FormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './payouts.component.html'
})
export class PayoutsComponent implements OnInit {
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges()||!!this.confirmation.current());}

 private readonly destroy=inject(DestroyRef);
 @ViewChild('form') private form?:NgForm;
 @Input({required:true}) organization!:Organization;@Input() admin=false;readonly api=inject(OrganizationApi);readonly payouts=signal<Payout[]>([]);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=statusLabel;
 readonly confirmation=new ConfirmationState();
 data={bankName:'',accountHolder:'',accountNumber:''};reason='';
 hasUnsavedChanges(){return !!(this.data.bankName||this.data.accountHolder||this.data.accountNumber||this.reason);}
 ngOnInit(){this.load();}
 load(){this.busy.set(true);this.error.set('');this.message.set('');this.api.payouts(this.organization.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:m=>this.payouts.set(m),error:e=>this.error.set(errorMessage(e))});}
 add(){if(this.busy()||!window.confirm('Gửi tài khoản nhận tiền này để quản trị viên xác minh?'))return;this.busy.set(true);this.error.set('');this.api.addPayout(this.organization.id,this.data).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:m=>{this.payouts.set(m);this.data={bankName:'',accountHolder:'',accountNumber:''};this.form?.resetForm(this.data);this.message.set('Đã gửi tài khoản nhận tiền. Cần quản trị viên xác minh trước khi sử dụng.');},error:e=>this.error.set(errorMessage(e))});}
 review(p:Payout,approve:boolean){
  if(this.busy()||!this.reason.trim())return;const reason=this.reason.trim();
  this.confirmation.ask({title:approve?'Xác minh tài khoản nhận tiền?':'Từ chối tài khoản nhận tiền?',subject:this.organization.name+' · '+p.bankName+' · '+p.maskedAccount+' · phiên bản '+p.version,
   description:approve?'Phiên bản tài khoản này được xác minh cho các khoản chi trả mới.':'Phiên bản này sẽ không được sử dụng cho chi trả mới.',reason,confirmLabel:approve?'Xác minh tài khoản':'Từ chối tài khoản',danger:!approve},()=>{
   this.busy.set(true);this.error.set('');this.api.reviewPayout(this.organization.id,p.id,approve,reason).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:m=>{this.payouts.set(m);this.reason='';this.message.set('Đã lưu quyết định xác minh.');},error:e=>this.error.set(errorMessage(e))});
  });
 }
}
