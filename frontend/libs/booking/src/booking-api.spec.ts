import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BookingApi,requestKey,clearRequestKey } from './booking-api';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
describe('Phase 3 API commands',()=>{
 let api:BookingApi;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({ imports: [], providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()] });api=TestBed.inject(BookingApi);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('sends selection identity and no client-priced order total',()=>{api.reserve('event','revision',[{ticketTypeId:'ticket',quantity:2}],'request-123').subscribe();const r=http.expectOne('/api/v1/orders/reservations');expect(r.request.headers.get('Idempotency-Key')).toBe('request-123');expect(r.request.body).toEqual({eventId:'event',selectionVersion:'revision',lines:[{ticketTypeId:'ticket',quantity:2}],termsAccepted:true});r.flush({});});
 it('never sends amount or user-controlled return URL to the provider',()=>{api.startPayment('order','request-456').subscribe();const r=http.expectOne('/api/v1/orders/order/payment-attempts');expect(r.request.body).toEqual({});expect(r.request.headers.get('Idempotency-Key')).toBe('request-456');r.flush({});});
 it('method selection never sends an outcome, delay, amount or card input for QR',()=>{api.selectPaymentMethod('attempt','QR').subscribe();const r=http.expectOne('/api/v1/payment-sessions/attempt/method');expect(r.request.body).toEqual({method:'QR'});r.flush({});});
 it('retains a request key across ambiguous response and removes it only explicitly',()=>{const scope='test-'+crypto.randomUUID();const key=requestKey(scope);expect(requestKey(scope)).toBe(key);clearRequestKey(scope);expect(requestKey(scope)).not.toBe(key);clearRequestKey(scope);});
});
