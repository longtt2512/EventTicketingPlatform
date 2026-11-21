import { refreshOnReturn } from '@event-ticketing/ui';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, ViewChild, Input, OnInit, inject, signal } from '@angular/core';
import { FORM_FEEDBACK, WORKSPACE } from '@event-ticketing/ui';
import { RouterLink } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { Organization, Member, EventDraft, Assignment, OrganizationApi, statusLabel } from './organization-api';
@Component({
    selector: 'etp-events', imports: [RouterLink, FormsModule, ...FORM_FEEDBACK], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './events.component.html'
})
export class EventsComponent implements OnInit {
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges());}

 private readonly destroy=inject(DestroyRef);readonly workspace=inject(WORKSPACE);
 @ViewChild('draft') private draftForm?:NgForm;
 @ViewChild('assignment') private assignmentForm?:NgForm;
 @Input({required:true}) organization!:Organization;readonly api=inject(OrganizationApi);readonly events=signal<EventDraft[]>([]);readonly staff=signal<Member[]>([]);readonly assignments=signal<Assignment[]>([]);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly label=statusLabel;name='';eventId='';userId='';
 hasUnsavedChanges(){return !!(this.name||this.eventId||this.userId);}
 ngOnInit(){this.load();}
 load(){this.busy.set(true);this.error.set('');forkJoin({events:this.api.events(this.organization.id),members:this.api.members(this.organization.id),assignments:this.api.assignments(this.organization.id)}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.events.set(r.events);this.staff.set(r.members.filter(m=>m.permission==='CHECKIN'&&m.status==='ACTIVE'));this.assignments.set(r.assignments);},error:e=>this.error.set(errorMessage(e))});}
 create(){if(this.busy())return;this.busy.set(true);this.error.set('');this.api.createEvent(this.organization.id,this.name).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:e=>{this.events.update(events=>[...events,e]);this.name='';this.draftForm?.resetForm({name:''});this.eventId=e.id;this.message.set('Đã tạo sự kiện nháp.');},error:e=>this.error.set(errorMessage(e))});}
 assign(){if(this.busy())return;this.busy.set(true);this.error.set('');this.api.assign(this.organization.id,this.eventId,this.userId).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.eventId='';this.userId='';this.assignmentForm?.resetForm({eventId:'',userId:''});this.message.set('Đã phân công nhân viên.');this.load();},error:e=>this.error.set(errorMessage(e))});}
 revoke(a:Assignment){if(this.busy()||!window.confirm('Thu hồi quyền nhân viên tại sự kiện này?'))return;this.busy.set(true);this.error.set('');this.api.unassign(this.organization.id,a.eventId,a.userId).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.assignments.update(items=>items.map(x=>x.id===a.id?{...x,status:'REVOKED'}:x));this.message.set('Đã thu hồi phân công.');},error:e=>this.error.set(errorMessage(e))});}
 eventName(id:string){return this.events().find(e=>e.id===id)?.name??id;}
 staffName(id:string){const s=this.staff().find(m=>m.userId===id);return s?.email??id;}
}
