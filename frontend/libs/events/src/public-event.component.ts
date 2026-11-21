import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventApi,eventError } from './event-api';
import { EventDetail,label,money,dateTime } from './event-models';
import { IconComponent } from '@event-ticketing/ui';
import { categoryName } from './event-art';
import { EventImageComponent } from './event-image.component';
@Component({
    selector: 'etp-public-event', imports: [RouterLink, EventImageComponent, IconComponent], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './public-event.component.html'
})
export class PublicEventComponent {
 private readonly api=inject(EventApi);private readonly route=inject(ActivatedRoute);readonly event=signal<EventDetail|null>(null);readonly error=signal('');readonly categoryName=categoryName;readonly label=label;readonly money=money;readonly date=dateTime;
 constructor(){this.api.publicDetail(this.route.snapshot.paramMap.get('id')!).pipe(takeUntilDestroyed()).subscribe({next:e=>this.event.set(e),error:e=>this.error.set(eventError(e))});}
}
