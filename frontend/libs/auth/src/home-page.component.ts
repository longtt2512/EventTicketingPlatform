import { AuthApiService } from '@event-ticketing/api-client';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin, finalize, catchError, of, throwError } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { IconComponent, WORKSPACE, WORKSPACE_NAMES } from '@event-ticketing/ui';
import { Organization, OrganizationApi, Application, statusLabel } from '../../organization/src/organization-api';
import { EventApi } from '../../events/src/event-api';
import { EventDetail } from '../../events/src/event-models';
import { errorMessage } from './feedback';

@Component({
    selector: 'etp-home-page', imports: [RouterLink, IconComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './home-page.component.html'
})
export class HomePageComponent {
 readonly session=inject(SessionStore);readonly workspace=inject(WORKSPACE);readonly names=WORKSPACE_NAMES;
 private readonly orgApi=inject(OrganizationApi);private readonly eventApi=inject(EventApi);private readonly destroy=inject(DestroyRef);
 readonly organizations=signal<Organization[]>([]);readonly applications=signal<Application[]>([]);readonly events=signal<EventDetail[]>([]);readonly moreEvents=signal(false);
 readonly busy=signal(false);readonly error=signal('');readonly label=statusLabel;
 readonly application=signal<Application|null>(null);readonly assigned=signal(0);readonly accessRefreshing=signal(false);private readonly auth=inject(AuthApiService);
 readonly pending=computed(()=>this.applications().filter(a=>a.status==='PENDING_VERIFICATION'));
 readonly active=computed(()=>this.organizations().filter(o=>o.status==='ACTIVE').length);
 readonly suspended=computed(()=>this.organizations().filter(o=>o.status==='SUSPENDED').length);
 readonly owned=computed(()=>this.organizations().filter(o=>o.owner).length);
 constructor(){if(this.workspace==='admin'||this.workspace==='organizer'||this.workspace==='checkin')this.load();}
 load(){this.busy.set(true);this.error.set('');
  if(this.workspace==='admin'){
   forkJoin({organizations:this.orgApi.organizations(),applications:this.orgApi.applications(),events:this.eventApi.queue('PENDING_REVIEW')}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.organizations.set(r.organizations);this.applications.set(r.applications);this.events.set(r.events.data);this.moreEvents.set(!!r.events.nextCursor);},error:e=>this.error.set(errorMessage(e))});
  }else if(this.workspace==='checkin'){this.orgApi.staffEvents().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:events=>this.assigned.set(events.length),error:e=>this.error.set(errorMessage(e))});
  }else{this.orgApi.organizations().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.organizations.set(r);if(!r.length)this.loadApplication();},error:e=>this.error.set(errorMessage(e))});}
 }
 private loadApplication(){this.orgApi.mine().pipe(takeUntilDestroyed(this.destroy),catchError(e=>e.status===404?of(null):throwError(()=>e))).subscribe({next:a=>this.application.set(a),error:e=>this.error.set(errorMessage(e))});}
 refreshAccess(){if(this.accessRefreshing())return;this.accessRefreshing.set(true);this.auth.me().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.accessRefreshing.set(false))).subscribe({next:u=>{this.session.updateUser(u);this.load();},error:e=>this.error.set(errorMessage(e))});}

}
