import { refreshOnReturn } from '@event-ticketing/ui';
import { OrganizationWorkspaceComponent } from '../../organization/src/organization-workspace.component';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { dateTime, localTime, utcTime, label } from '../../events/src/event-models';
import { OperationsApi, operationError } from './operations-api';
import { ReportSummary, ReportRow, amount, percentage } from './operation-models';
@Component({
    selector: 'etp-analytics', imports: [OrganizationWorkspaceComponent, FormsModule], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './analytics.component.html', styleUrl: './operations.scss'
})
export class AnalyticsComponent {private expanded=false;
 readonly api=inject(OperationsApi);private readonly destroy=inject(DestroyRef);readonly org=inject(ActivatedRoute).snapshot.paramMap.get('org');
 readonly report=signal<ReportSummary|null>(null);readonly rows=signal<ReportRow[]>([]);readonly loading=signal(false);readonly pageBusy=signal(false);readonly exporting=signal(false);readonly error=signal('');readonly more=signal(false);readonly total=signal(0);
 readonly amount=amount;readonly percent=percentage;readonly date=dateTime;readonly label=label;
 readonly maxRevenue=computed(()=>Math.max(1,...(this.report()?.series.map(r=>r.grossVnd)??[])));
 timezone='Asia/Ho_Chi_Minh';from=localTime(new Date().toISOString()).slice(0,7)+'-01T00:00';to=localTime(new Date(Date.now()+86400000).toISOString()).slice(0,10)+'T00:00';eventId='';private applied:Record<string,string>={};private displayedFilters="";
 constructor(){refreshOnReturn(()=>this.load(),()=>this.loading()||this.pageBusy()||this.exporting()||this.expanded||this.displayedFilters!==JSON.stringify([this.from,this.to,this.timezone,this.eventId]));this.load();}
 load(){if(this.loading())return;let params:Record<string,string>;try{const from=utcTime(this.from,this.timezone),to=utcTime(this.to,this.timezone);if(!from||!to||from>=to)throw new Error('Chọn thời gian bắt đầu trước thời gian kết thúc.');params={from,to,timezone:this.timezone,dateBasis:'CONFIRMED_AT',...(this.eventId?{eventId:this.eventId}:{})};}catch(e){this.error.set(e instanceof Error?e.message:'Thời gian không hợp lệ.');return;}
 this.loading.set(true);this.error.set('');this.expanded=false;this.report.set(null);this.rows.set([]);this.more.set(false);
 this.api.summary(this.org,params).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.report.set(r);this.displayedFilters=JSON.stringify([this.from,this.to,this.timezone,this.eventId]);this.applied={...params,snapshotId:r.snapshotId};this.page();},error:e=>this.error.set(operationError(e))});}
 page(){if(this.pageBusy()||!this.report())return;this.expanded=this.rows().length>0;this.pageBusy.set(true);this.error.set('');this.api.sales(this.org,{...this.applied,offset:String(this.rows().length)}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.pageBusy.set(false))).subscribe({next:p=>{this.rows.update(rows=>[...rows,...p.data]);this.more.set(p.hasMore);this.total.set(p.total);},error:e=>this.error.set(operationError(e))});}
 export(){if(this.exporting()||!this.report())return;this.exporting.set(true);this.error.set('');this.api.csv(this.org,this.applied).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.exporting.set(false))).subscribe({next:blob=>{const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='doanh-thu-'+this.report()!.snapshotId+'.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);},error:e=>this.error.set(operationError(e))});}
}
