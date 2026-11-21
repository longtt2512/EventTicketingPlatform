import { refreshOnReturn } from '@event-ticketing/ui';
import { activityLabel, activityDetail } from '../../ui/src/activity-labels';
import { dateTime } from '../../events/src/event-models';
import { OrganizationWorkspaceComponent } from './organization-workspace.component';
import { ChangeDetectionStrategy, Component, DestroyRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { FORM_FEEDBACK, UnsavedChangesDirective, discardChangesMessage, ConfirmDialogComponent, ConfirmationState } from '@event-ticketing/ui';
import { errorMessage } from '../../auth/src/feedback';
import { Organization, OrganizationApi, Audit, statusLabel } from './organization-api';
import { MembersComponent } from './members.component';
import { PayoutsComponent } from './payouts.component';
import { EventsComponent } from './events.component';

@Component({
    selector: 'etp-organization-detail',
    imports: [OrganizationWorkspaceComponent, FormsModule, MembersComponent, PayoutsComponent, EventsComponent, ...FORM_FEEDBACK, UnsavedChangesDirective, ConfirmDialogComponent],
    changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './organization-detail.component.html'
})
export class OrganizationDetailComponent {
 readonly api=inject(OrganizationApi); readonly session=inject(SessionStore);
 private readonly route=inject(ActivatedRoute); private readonly router=inject(Router); private readonly destroy=inject(DestroyRef);
 readonly id=this.route.snapshot.paramMap.get('id')!;
 readonly organization=signal<Organization|null>(null); readonly tab=signal('general'); readonly visited=signal(new Set(['general']));
 readonly audit=signal<Audit[]>([]); readonly busy=signal(false); readonly error=signal(''); readonly message=signal(''); readonly label=statusLabel;
 readonly confirmation=new ConfirmationState();readonly actionLabel=activityLabel;readonly detailLabel=activityDetail;readonly date=dateTime;
 @ViewChild(MembersComponent) private members?:MembersComponent;
 @ViewChild(PayoutsComponent) private payouts?:PayoutsComponent;
 @ViewChild(EventsComponent) private events?:EventsComponent;
 reason=''; data={name:'',contactEmail:'',address:'',description:'',version:0}; private baseline=JSON.stringify(this.data);
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges()||!!this.confirmation.current());
   this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(params=>this.showSection(params.get('section')??'general'));
   this.load();
 }
 admin(){return this.session.user()?.roles.includes('ADMIN')??false;}
 canManage(){const o=this.organization();return this.admin()||o?.owner||o?.permission==='MANAGEMENT';}
 canFinance(){const o=this.organization();return this.admin()||o?.owner||o?.permission==='FINANCE';}
 hasUnsavedChanges():boolean{return JSON.stringify(this.data)!==this.baseline||!!this.reason||!!this.members?.hasUnsavedChanges()||!!this.payouts?.hasUnsavedChanges()||!!this.events?.hasUnsavedChanges();}
 load(){
   if(this.hasUnsavedChanges()&&!window.confirm(discardChangesMessage))return;
   this.busy.set(true);this.error.set('');this.message.set('');
   this.api.organization(this.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:o=>{this.receive(o);this.reason='';if(this.tab()==='audit')this.showSection('audit');},error:e=>this.error.set(errorMessage(e))});
 }
 receive(o:Organization){this.organization.set(o);this.data={name:o.name,contactEmail:o.contactEmail,address:o.address,description:o.description,version:o.version};this.baseline=JSON.stringify(this.data);}
 save(){
   if(this.busy())return;this.busy.set(true);this.error.set('');
   this.api.update(this.id,this.data).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:o=>{this.receive(o);this.message.set('Đã lưu thông tin tổ chức.');},error:e=>this.error.set(errorMessage(e))});
 }
 status(){
   const o=this.organization();if(!o||!this.reason.trim()||this.busy())return;
   const suspend=o.status==='ACTIVE', reason=this.reason.trim();
   this.confirmation.ask({title:suspend?'Đình chỉ tổ chức?':'Khôi phục tổ chức?',subject:o.name,description:suspend?'Các thay đổi của tổ chức và quyền truy cập sự kiện của nhân viên sẽ bị chặn.':'Tổ chức và nhân viên được tiếp tục sử dụng các quyền hiện có.',reason,confirmLabel:suspend?'Đình chỉ tổ chức':'Khôi phục tổ chức',danger:suspend},()=>{
     this.busy.set(true);this.error.set('');
     this.api.status(this.id,suspend,reason).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:v=>{this.receive(v);this.reason='';this.message.set('Đã cập nhật trạng thái tổ chức.');},error:e=>this.error.set(errorMessage(e))});
   });
 }
 select(section:string){void this.router.navigate([], {relativeTo:this.route,queryParams:{section},queryParamsHandling:'merge'});}
 private showSection(section:string){
   const valid=['general','members','payouts','events','audit'].includes(section)?section:'general';
   this.tab.set(valid);this.visited.update(previous=>new Set([...previous,valid]));
   if(valid==='audit')this.api.audit(this.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:a=>this.audit.set(a),error:e=>this.error.set(errorMessage(e))});
 }
}
