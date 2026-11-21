import { activityLabel, activitySource, activityFields, activityOptions } from '../../ui/src/activity-labels';
import { ChangeDetectionStrategy,Component,DestroyRef,inject,signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { dateTime,utcTime } from '../../events/src/event-models';
import { OperationsApi,operationError } from './operations-api';
import { AuditPage,AuditRecord } from './operation-models';
@Component({
    selector: 'etp-audit', imports: [FormsModule], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './audit.component.html', styleUrl: './operations.scss'
})
export class AuditComponent {
 readonly api=inject(OperationsApi);private readonly destroy=inject(DestroyRef);readonly rows=signal<AuditRecord[]>([]);readonly sources=signal<AuditPage['sources']>([]);readonly next=signal<string|null>(null);readonly selected=signal<AuditRecord|null>(null);readonly busy=signal(false);readonly error=signal('');readonly date=dateTime;readonly actionLabel=activityLabel;readonly sourceLabel=activitySource;readonly fields=activityFields;readonly actions=activityOptions();
 action='';actorId='';resourceId='';from='';to='';private filters:Record<string,string>={};
 constructor(){this.load();}
 load(more=false){if(this.busy())return;if(!more){try{const from=utcTime(this.from),to=utcTime(this.to);if(from&&to&&from>=to)throw new Error();this.filters={...(this.action.trim()?{action:this.action.trim()}:{}),...(this.actorId.trim()?{actorId:this.actorId.trim()}:{}),...(this.resourceId.trim()?{resourceId:this.resourceId.trim()}:{}),...(from?{from}:{}),...(to?{to}:{})};}catch{this.error.set('Khoảng thời gian không hợp lệ.');return;}}this.busy.set(true);this.error.set('');this.api.audit({...this.filters,...(more&&this.next()?{cursor:this.next()!}:{})}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:p=>{this.rows.set(more?[...this.rows(),...p.data]:p.data);this.next.set(p.nextCursor);this.sources.set(p.sources);},error:e=>this.error.set(operationError(e))});}
 open(row:AuditRecord){this.selected.set(row);}
}
