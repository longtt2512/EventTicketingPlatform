import { ChangeDetectionStrategy, Component, DestroyRef, Input, OnChanges, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { EventApi } from './event-api';
import { categoryArt } from './event-art';
@Component({
 selector:'etp-event-image', standalone:true, changeDetection:ChangeDetectionStrategy.OnPush,
 template:`@if(url()){<img [src]="url()" [alt]="alt" [attr.loading]="priority?'eager':'lazy'" (error)="url.set('')">}@else{<div class="event-image-empty" [style.background-image]="'url('+art(category)+')'"><span>{{title||'ETP'}}</span>@if(!title){<small>{{mediaId?'Ảnh chưa tải được':'Chưa có ảnh bìa'}}</small>}</div>}`,
 styles:[`:host{display:block;overflow:hidden;background:#22145d;aspect-ratio:16/9;border-radius:10px}img{display:block;width:100%;height:100%;object-fit:cover}.event-image-empty{height:100%;display:flex;flex-direction:column;justify-content:flex-end;gap:12px;color:white;padding:8%;background-size:cover;background-position:center;position:relative}.event-image-empty::before{content:'';position:absolute;inset:0;background:linear-gradient(90deg,rgba(20,12,55,.5),transparent)}span,small{position:relative;max-width:20ch}span{font-size:clamp(1.1rem,2.5vw,2.6rem);font-weight:800;line-height:1.05;letter-spacing:-.035em;text-transform:uppercase;overflow-wrap:anywhere}small{font-size:.75rem;color:#ede9fe}`]
})
export class EventImageComponent implements OnChanges {
 @Input() mediaId:string|null=null; @Input() alt=''; @Input() title=''; @Input() category:string|null=null; @Input() priority=false;
 readonly url=signal(''); readonly art=categoryArt; private api=inject(EventApi); private request?:Subscription;
 constructor(){inject(DestroyRef).onDestroy(()=>this.request?.unsubscribe());}
 ngOnChanges(){this.request?.unsubscribe();this.url.set('');if(this.mediaId)this.request=this.api.media(this.mediaId).subscribe({next:r=>this.url.set(r.url),error:()=>this.url.set('')});}
}
