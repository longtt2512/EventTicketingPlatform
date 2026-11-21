import {TestBed} from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import {provideRouter} from '@angular/router';
import {HomeDiscoveryComponent} from './home-discovery.component';
import {EventImageComponent} from './event-image.component';

describe('Mockup home with live discovery',()=>{
 let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('shows real empty state without advertising a fabricated event',()=>{
  const f=TestBed.createComponent(HomeDiscoveryComponent);f.detectChanges();
  expect(f.nativeElement.querySelector('[role=status]')).not.toBeNull();
  http.expectOne(r=>r.url.endsWith('/discover/events')&&r.params.get('sort')==='upcoming').flush({data:[],nextCursor:null});f.detectChanges();
  expect(f.nativeElement.textContent).toContain('Những trải nghiệm mới');
  expect(f.nativeElement.querySelector('etp-event-card')).toBeNull();
  expect(f.nativeElement.querySelector('a.hero-secondary')).toBeNull();
 });
 it('recovers a failed home request without retaining a loading skeleton',()=>{
  const f=TestBed.createComponent(HomeDiscoveryComponent);f.detectChanges();
  http.expectOne(r=>r.url.endsWith('/discover/events')).error(new ProgressEvent('network'));f.detectChanges();
  expect(f.nativeElement.querySelector('[role=alert]')).not.toBeNull();
  expect(f.nativeElement.querySelector('.event-skeleton')).toBeNull();
  f.nativeElement.querySelector('.notice button').click();
  http.expectOne(r=>r.url.endsWith('/discover/events')).flush({data:[],nextCursor:null});f.detectChanges();
  expect(f.nativeElement.querySelector('[role=alert]')).toBeNull();
 });
 it('uses the event identity for links and prefers a featured event in the loaded collection',()=>{
  const f=TestBed.createComponent(HomeDiscoveryComponent);f.detectChanges();
  const event={id:'first',name:'Tên sự kiện thật',category:'Workshop',bannerMediaId:null,startAt:'2027-01-02T12:00:00Z',timezone:'Asia/Ho_Chi_Minh',city:'Hà Nội',minPrice:0,featured:false};
  http.expectOne(r=>r.url.endsWith('/discover/events')).flush({data:[event,{...event,id:'featured',name:'Sự kiện nổi bật',featured:true}],nextCursor:null});f.detectChanges();
  expect(f.componentInstance.hero()?.id).toBe('featured');
  expect(f.nativeElement.querySelector('a.hero-secondary').getAttribute('href')).toBe('/events/featured');
  expect(f.nativeElement.querySelectorAll('etp-event-card').length).toBe(2);
  expect(f.nativeElement.textContent).toContain('Miễn phí');
 });
 it('prioritizes uploaded media and falls back to text-free supplied art after failure',()=>{
  const f=TestBed.createComponent(EventImageComponent);f.componentRef.setInput('mediaId','media');f.componentRef.setInput('title','Sự kiện thật');f.componentRef.setInput('category','Sports');f.detectChanges();
  http.expectOne('/api/v1/media/media/download').flush({url:'/real-cover.png'});f.detectChanges();
  expect(f.nativeElement.querySelector('img').getAttribute('src')).toBe('/real-cover.png');
  f.nativeElement.querySelector('img').dispatchEvent(new Event('error'));f.detectChanges();
  expect(f.nativeElement.querySelector('.event-image-empty').style.backgroundImage).toContain('city-night-run.png');
  expect(f.nativeElement.textContent).toContain('Sự kiện thật');
 });
});
