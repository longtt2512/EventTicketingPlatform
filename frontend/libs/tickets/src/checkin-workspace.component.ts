import { refreshOnReturn } from '@event-ticketing/ui';
import {ChangeDetectionStrategy,Component,DestroyRef,ViewChild,inject,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute,RouterLink} from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {IconComponent,ConfirmDialogComponent,ConfirmationState} from '@event-ticketing/ui';
import {TicketApi,ticketError} from './ticket-api';
import {EventAccess,Decision,LookupResult,Admission,Stats,decisionCopy,ticketLabel} from './ticket-models';
import {CameraScannerComponent} from './camera-scanner.component';
import {CheckinOperation} from './checkin-operation';
import {dateTime} from '../../events/src/event-models';
@Component({
    selector: 'etp-checkin-workspace', imports: [FormsModule, RouterLink, CameraScannerComponent, ConfirmDialogComponent, IconComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './checkin-workspace.component.html'
})
export class CheckinWorkspaceComponent {private expanded=false;
 private readonly api=inject(TicketApi);private readonly route=inject(ActivatedRoute);private readonly destroy=inject(DestroyRef);readonly eventId=this.route.snapshot.paramMap.get('eventId')!;readonly mode=this.route.snapshot.data['mode'] as 'scan'|'lookup'|'history'|'stats';
 @ViewChild(CameraScannerComponent)camera?:CameraScannerComponent;
 readonly context=signal<EventAccess|null>(null);readonly busy=signal(false);readonly loading=signal(false);readonly error=signal('');readonly unknown=signal(false);readonly result=signal<Decision|null>(null);readonly lookup=signal<LookupResult|null>(null);readonly history=signal<Admission[]>([]);readonly cursor=signal<string|null>(null);readonly stats=signal<Stats|null>(null);readonly online=signal(navigator.onLine);readonly operation=new CheckinOperation();readonly confirmation=new ConfirmationState();readonly date=dateTime;readonly label=ticketLabel;readonly copy=decisionCopy;ticketId='';
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.loading()||this.busy()||!!this.operation.current||!!this.confirmation.current()||!!this.ticketId||this.expanded);const status=()=>{this.online.set(navigator.onLine);if(!navigator.onLine)this.camera?.stop();};const warn=(e:BeforeUnloadEvent)=>{if(this.operation.current){e.preventDefault();e.returnValue='';}};window.addEventListener('online',status);window.addEventListener('offline',status);window.addEventListener('beforeunload',warn);this.destroy.onDestroy(()=>{window.removeEventListener('online',status);window.removeEventListener('offline',status);window.removeEventListener('beforeunload',warn);this.operation.resolved();});void this.load();}
 async load(more=false){if(this.loading()||this.busy())return;this.expanded=more;this.loading.set(true);this.error.set('');try{this.context.set(await firstValueFrom(this.api.event(this.eventId).pipe(takeUntilDestroyed(this.destroy))));if(this.mode==='stats')this.stats.set(await firstValueFrom(this.api.stats(this.eventId).pipe(takeUntilDestroyed(this.destroy))));if(this.mode==='history'){const p=await firstValueFrom(this.api.history(this.eventId,more?this.cursor()??undefined:undefined).pipe(takeUntilDestroyed(this.destroy)));this.history.set(more?[...this.history(),...p.data]:p.data);this.cursor.set(p.nextCursor);}}catch(e){this.error.set(ticketError(e));this.context.set(null);this.camera?.stop();}finally{this.loading.set(false);}}
 async search(){if(this.busy()||this.operation.current)return;this.lookup.set(null);this.result.set(null);this.error.set('');if(!/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i.test(this.ticketId.trim())){this.error.set('Nhập đầy đủ mã vé gồm 36 ký tự, có dấu gạch ngang.');return;}this.busy.set(true);try{this.lookup.set(await firstValueFrom(this.api.lookup(this.eventId,this.ticketId.trim()).pipe(takeUntilDestroyed(this.destroy))));}catch(e){this.error.set(ticketError(e));}finally{this.busy.set(false);}}
 confirm(){const t=this.lookup();if(!t?.canCheckIn||this.busy()||!this.online()||this.operation.current)return;this.confirmation.ask({title:'Xác nhận khách vào cửa?',subject:t.buyerName+' · '+t.ticketTypeName,description:'Vé sẽ được đánh dấu đã sử dụng. Hãy đối chiếu đúng mã vé với khách.',confirmLabel:'Xác nhận check-in'},()=>{if(this.operation.begin('MANUAL',t.ticketId))void this.submit();});}
 scanned(value:string){if(!this.online()||this.busy()||this.result()||!this.operation.begin('QR',value))return;void this.submit();}
 async submit(){if(this.busy()||!this.online())return;const op=this.operation.current;if(!op)return;this.busy.set(true);this.unknown.set(false);this.error.set('');this.camera?.stop();try{const request=op.kind==='QR'?this.api.scan(this.eventId,op.value,op.key):this.api.confirm(this.eventId,op.value,op.key);this.result.set(await firstValueFrom(request.pipe(takeUntilDestroyed(this.destroy))));this.lookup.set(null);this.operation.resolved();}catch(e){this.unknown.set(!(e instanceof HttpErrorResponse)||e.status===0||e.status>=500);this.error.set(ticketError(e));if(!this.unknown())this.operation.resolved();}finally{this.busy.set(false);}}
 next(){if(this.operation.current||this.busy())return;this.result.set(null);this.lookup.set(null);this.ticketId='';this.error.set('');void this.load();}
}
