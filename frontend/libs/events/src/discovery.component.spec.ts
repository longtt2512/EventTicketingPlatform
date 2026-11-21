import {TestBed,fakeAsync,tick} from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import {ActivatedRoute,Router,convertToParamMap,provideRouter} from '@angular/router';
import {BehaviorSubject} from 'rxjs';
import {DiscoveryComponent} from './discovery.component';
describe('Discovery query recovery',()=>{
 it('preserves numeric zero price and handles malformed date links',fakeAsync(()=>{
  const query=new BehaviorSubject(convertToParamMap({from:'invalid-date'}));
  TestBed.configureTestingModule({imports:[DiscoveryComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{queryParamMap:query}}]});
  const fixture=TestBed.createComponent(DiscoveryComponent),http=TestBed.inject(HttpTestingController);fixture.detectChanges();
  http.expectOne('/api/v1/discover/categories').flush(['Workshop']);
  http.expectOne(r=>r.url.endsWith('/discover/events')&&r.params.get('from')==='invalid-date').flush({code:'VALIDATION_ERROR'},{status:400,statusText:'Bad query'});
  expect(fixture.componentInstance.error()).toContain('kiểm tra');
  const navigate=spyOn(TestBed.inject(Router),'navigate').and.returnValue(Promise.resolve(true));fixture.componentInstance.maxPrice=0 as unknown as string;fixture.componentInstance.from='';fixture.componentInstance.apply();tick();
  expect(navigate.calls.mostRecent().args[1]?.queryParams?.['maxPrice']).toBe('0');http.verify();fixture.destroy();
 }));
});
