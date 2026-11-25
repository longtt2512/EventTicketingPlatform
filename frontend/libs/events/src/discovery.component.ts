import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY,catchError,finalize,switchMap } from 'rxjs';
import { EventApi,eventError } from './event-api';
import { CATEGORIES,EventDetail,money,dateTime } from './event-models';
import { EventCardComponent } from './event-card.component';
import { categoryName } from './event-art';
import { IconComponent, FORM_FEEDBACK } from '@event-ticketing/ui';
@Component({
    selector: 'etp-discovery', imports: [FormsModule, EventCardComponent, IconComponent, ...FORM_FEEDBACK], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './discovery.component.html'
})
export class DiscoveryComponent {
 private readonly api=inject(EventApi);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
 readonly filtersOpen=signal(false);readonly categoryName=categoryName;readonly events=signal<EventDetail[]>([]);readonly busy=signal(false);readonly error=signal('');readonly next=signal<string|null>(null);readonly categories=signal<string[]>([]);readonly money=money;readonly date=dateTime;
 q='';category='';city='';from='';to='';minPrice:string|number|null='';maxPrice:string|number|null='';sort='upcoming';featured=false;private params:Record<string,string>={};
 constructor(){this.api.categories().pipe(takeUntilDestroyed(this.destroy)).subscribe({next:items=>this.categories.set(items),error:()=>this.error.set('Chưa tải được danh mục. Tải lại trang để thử lại.')});this.route.queryParamMap.pipe(switchMap(p=>{
  this.q=p.get('q')??'';this.category=p.get('category')??'';this.city=p.get('city')??'';this.from=p.get('from')??'';this.to=p.get('to')??'';this.minPrice=p.get('minPrice')??'';this.maxPrice=p.get('maxPrice')??'';this.sort=p.get('sort')??'upcoming';this.featured=p.get('featured')==='true';
  this.params=this.searchParams();this.busy.set(true);this.error.set('');this.events.set([]);this.next.set(null);
  return this.api.search(this.params).pipe(catchError(e=>{this.error.set(eventError(e));return EMPTY;}),finalize(()=>this.busy.set(false)));
 }),takeUntilDestroyed()).subscribe(result=>{this.events.set(result.data);this.next.set(result.nextCursor);});}
 private dateFilter(value:string,time:string){const date=new Date(value+'T'+time+'+07:00');return Number.isNaN(date.getTime())?value:date.toISOString();}
 private searchParams(){const p:Record<string,string>={sort:this.sort};for(const k of ['q','category','city','minPrice','maxPrice'] as const)if(this[k]!==''&&this[k]!=null)p[k]=String(this[k]);if(this.from)p['from']=this.dateFilter(this.from,'00:00:00');if(this.to)p['to']=this.dateFilter(this.to,'23:59:59');if(this.featured)p['featured']='true';return p;}
 apply(){if([this.minPrice,this.maxPrice].some(v=>v!==''&&v!=null&&(!Number.isSafeInteger(Number(v))||Number(v)<0))){this.error.set('Nhập giá vé VNĐ nguyên, không âm.');return;}if(this.minPrice!==''&&this.minPrice!=null&&this.maxPrice!==''&&this.maxPrice!=null&&Number(this.minPrice)>Number(this.maxPrice)){this.error.set('Giá đến phải lớn hơn hoặc bằng giá từ.');return;}this.filtersOpen.set(false);const query:Record<string,string|null>={sort:this.sort,featured:this.featured?'true':null};for(const k of ['q','category','city','from','to','minPrice','maxPrice'] as const)query[k]=this[k]===''||this[k]==null?null:String(this[k]);void this.router.navigate([],{relativeTo:this.route,queryParams:query}).then(changed=>{if(!changed)this.reload();});}
 tab(sort:string,featured=false){this.sort=sort;this.featured=featured;this.apply();}
 reload(){this.error.set('');this.busy.set(true);this.api.search(this.params).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.events.set(r.data);this.next.set(r.nextCursor);},error:e=>this.error.set(eventError(e))});}
 clear(){this.q=this.category=this.city=this.from=this.to=this.minPrice=this.maxPrice='';this.sort='upcoming';this.featured=false;this.apply();}
 loadMore(){const cursor=this.next();if(!cursor||this.busy())return;const identity=JSON.stringify(this.params);this.busy.set(true);this.api.search({...this.params,cursor}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{if(identity!==JSON.stringify(this.params))return;this.events.update(items=>[...items,...r.data]);this.next.set(r.nextCursor);},error:e=>this.error.set(eventError(e))});}
}
