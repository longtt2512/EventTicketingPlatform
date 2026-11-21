import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '@event-ticketing/ui';
import { EventDetail, money } from './event-models';
import { categoryName, eventDay } from './event-art';
import { EventImageComponent } from './event-image.component';
@Component({
    selector: 'etp-event-card', imports: [RouterLink, IconComponent, EventImageComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `<article class="event-card" [attr.data-category]="event.category"><a [routerLink]="['/events',event.id]" [attr.aria-label]="event.name">
 <div class="event-card-art"><etp-event-image [mediaId]="event.bannerMediaId" [title]="event.name" [category]="event.category"/><span class="event-category-tag">{{category(event.category)}}</span></div>
 <div class="event-card-copy"><p class="event-card-meta"><etp-icon name="calendar"/>{{day(event.startAt,event.timezone)}} · {{event.city}}</p><h2>{{event.name}}</h2>@if(event.featured){<p class="event-card-category">Sự kiện nổi bật</p>}<div class="event-card-bottom"><span>@if(event.minPrice!==0){<small>Từ </small>}<strong>{{money(event.minPrice)}}</strong></span><etp-icon name="arrow-right" tone="indigo"/></div></div></a></article>`
})
export class EventCardComponent {
 @Input({required:true}) event!:EventDetail;
 readonly money=money; readonly day=eventDay; readonly category=categoryName;
}
