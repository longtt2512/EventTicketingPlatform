import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { BookingApi } from './booking-api';
import { BookingQuote } from './booking-models';
import { BookingComponent } from './booking.component';

describe('Booking after the last ticket is held by another buyer', () => {
 it('allows removing an unavailable selected line while preventing an increase', async () => {
  const ticket='12345678-1234-1234-1234-123456789012';
  const quote={selectionVersion:'current',serverTime:'2030-01-01T00:00:00Z',event:{id:'event',name:'Local event',category:'Workshop',venueName:'Local venue',startAt:'2030-02-01T00:00:00Z',timezone:'Asia/Ho_Chi_Minh',terms:'Terms',bannerMediaId:null,ticketTypes:[{id:ticket,name:'Standard',description:'',price:1000,available:0,maxPerOrder:2,saleState:'SOLD_OUT'}]}} as unknown as BookingQuote;
  const api=jasmine.createSpyObj<BookingApi>('BookingApi',['quote','reserve']);api.quote.and.returnValue(of(quote));
  TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),
   {provide:BookingApi,useValue:api},{provide:SessionStore,useValue:{isAuthenticated:()=>true,user:()=>({id:'buyer',emailVerified:true})}},
   {provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({eventId:'event'}),queryParamMap:convertToParamMap({selection:ticket+':1'})}}}
  ]});
  const fixture=TestBed.createComponent(BookingComponent);await fixture.whenStable();fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
  const minus=fixture.nativeElement.querySelector('[aria-label="Giảm số lượng Standard"]') as HTMLButtonElement;
  const plus=fixture.nativeElement.querySelector('[aria-label="Tăng số lượng Standard"]') as HTMLButtonElement;
  expect(plus.disabled).toBeTrue();expect(minus.disabled).toBeFalse();
  minus.click();fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
  expect(fixture.componentInstance.count()).toBe(0);expect(fixture.componentInstance.total()).toBe(0);
  expect(fixture.componentInstance.terms).toBeFalse();expect(api.reserve).not.toHaveBeenCalled();expect(minus.disabled).toBeTrue();
 });
});
