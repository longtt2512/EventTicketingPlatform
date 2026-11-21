import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { IconComponent } from '@event-ticketing/ui';
import { EventApi, eventError } from './event-api';
import { EventDetail } from './event-models';
import { EventCardComponent } from './event-card.component';
import { categoryArt, categoryName, eventDay } from './event-art';
@Component({
    selector: 'etp-home-discovery', imports: [RouterLink, IconComponent, EventCardComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './home-discovery.component.html'
})
export class HomeDiscoveryComponent {
 private readonly api=inject(EventApi); private readonly destroy=inject(DestroyRef);
 readonly events=signal<EventDetail[]>([]); readonly busy=signal(false); readonly error=signal('');
 readonly hero=computed(()=>this.events().find(e=>e.featured)??this.events()[0]); readonly day=eventDay;readonly art=categoryArt;readonly categoryName=categoryName;
 readonly categories=[{name:'Âm nhạc',value:'Music & Concert',icon:'music'},{name:'Hội nghị',value:'Conference',icon:'users'},{name:'Workshop',value:'Workshop',icon:'pencil'},{name:'Thể thao',value:'Sports',icon:'flag'},{name:'Triển lãm',value:'Exhibition',icon:'image'},{name:'Giải trí',value:'Entertainment',icon:'star'}];
 constructor(){this.load();}
 load(){this.busy.set(true);this.error.set('');this.api.search({sort:'upcoming'}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>this.events.set(r.data),error:e=>this.error.set(eventError(e))});}
}
