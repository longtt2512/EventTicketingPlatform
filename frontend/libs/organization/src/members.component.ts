import { refreshOnReturn } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, ViewChild, Input, OnInit, inject, signal } from '@angular/core';
import { FORM_FEEDBACK } from '@event-ticketing/ui';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { Organization, Member, OrganizationApi, statusLabel } from './organization-api';
@Component({
    selector: 'etp-members', imports: [FormsModule, ...FORM_FEEDBACK], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './members.component.html'
})
export class MembersComponent implements OnInit {
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges());}

 private readonly destroy=inject(DestroyRef);
 @ViewChild('form') private form?:NgForm;
 @Input({required:true}) organization!:Organization;readonly api=inject(OrganizationApi);readonly members=signal<Member[]>([]);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=statusLabel;email='';permission='CHECKIN';
 hasUnsavedChanges(){return !!this.email||this.permission!=='CHECKIN';}
 ngOnInit(){this.load();}
 load(){this.busy.set(true);this.error.set('');this.message.set('');this.api.members(this.organization.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:m=>this.members.set(m),error:e=>this.error.set(errorMessage(e))});}
 add(){if(this.busy())return;this.busy.set(true);this.error.set('');this.api.addMember(this.organization.id,this.email,this.permission).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:m=>{this.members.set(m);this.email='';this.permission='CHECKIN';this.form?.resetForm({email:'',permission:'CHECKIN'});this.message.set('Đã thêm thành viên.');},error:e=>this.error.set(errorMessage(e))});}
 revoke(m:Member){if(this.busy()||!window.confirm('Thu hồi thành viên và toàn bộ phân công sự kiện của người này?'))return;this.busy.set(true);this.error.set('');this.api.revokeMember(this.organization.id,m.userId).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.members.update(items=>items.map(x=>x.userId===m.userId?{...x,status:'REVOKED'}:x));this.message.set('Đã thu hồi thành viên và quyền sự kiện.');},error:e=>this.error.set(errorMessage(e))});}
}
