import { ChangeDetectionStrategy,Component,DestroyRef,inject,signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Order,orderLabel } from '../../booking/src/booking-models';
import { dateTime } from '../../events/src/event-models';
import { OperationsApi,operationError } from './operations-api';
import { amount } from './operation-models';
@Component({
    selector: 'etp-admin-orders', imports: [FormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './admin-orders.component.html', styleUrl: './operations.scss'
})
export class AdminOrdersComponent {
 readonly api=inject(OperationsApi);private readonly destroy=inject(DestroyRef);readonly rows=signal<Order[]>([]);readonly next=signal<string|null>(null);readonly busy=signal(false);readonly error=signal('');readonly amount=amount;readonly label=orderLabel;readonly date=dateTime;status='';private appliedStatus='';
 constructor(){this.load();}load(more=false){if(this.busy())return;if(!more)this.appliedStatus=this.status;this.busy.set(true);this.error.set('');this.api.orders(this.appliedStatus,more?this.next()??undefined:undefined).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:p=>{this.rows.set(more?[...this.rows(),...p.data]:p.data);this.next.set(p.nextCursor);},error:e=>this.error.set(operationError(e))});}
}
