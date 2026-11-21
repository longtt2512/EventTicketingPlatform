import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { API_BASE_URL } from '@event-ticketing/api-client';
import { errorMessage } from '../../auth/src/feedback';
import { AdminAttempt,Attempt,CheckoutSession,BookingQuote,Order,Page,PaymentCase,PaymentStatus } from './booking-models';
@Injectable({providedIn:'root'})
export class BookingApi {
 private readonly http=inject(HttpClient);private readonly base=API_BASE_URL;
 quote(id:string){return this.http.get<BookingQuote>(`${this.base}/events/${id}/booking`);}
 reserve(eventId:string,selectionVersion:string,lines:{ticketTypeId:string;quantity:number}[],key:string){return this.http.post<Order>(`${this.base}/orders/reservations`,{eventId,selectionVersion,lines,termsAccepted:true},{headers:{'Idempotency-Key':key}});}
 order(id:string){return this.http.get<Order>(`${this.base}/orders/${id}`);}
 orders(status='',cursor?:string,org?:string,event?:string){return this.http.get<Page<Order>>(org&&event?`${this.base}/organizations/${org}/events/${event}/orders`:`${this.base}/orders`,{params:{...(status?{status}:{}),...(cursor?{cursor}:{})}});}
 scopedOrder(org:string,event:string,id:string){return this.http.get<Order>(`${this.base}/organizations/${org}/events/${event}/orders/${id}`);}
 command(id:string,action:'cancel'|'confirm-free',key:string){return this.http.post<Order>(`${this.base}/orders/${id}/${action}`,{},{headers:{'Idempotency-Key':key}});}
 startPayment(id:string,key:string){return this.http.post<Attempt>(`${this.base}/orders/${id}/payment-attempts`,{},{headers:{'Idempotency-Key':key}});}
 paymentStatus(id:string){return this.http.get<PaymentStatus>(`${this.base}/orders/${id}/payment-status`);}
 paymentSession(id:string){return this.http.get<CheckoutSession>(`${this.base}/payment-sessions/${id}`);}
 selectPaymentMethod(id:string,method:'QR'|'CARD',cardNumber?:string){return this.http.post<CheckoutSession>(`${this.base}/payment-sessions/${id}/method`,{method,...(cardNumber?{cardNumber}:{})});}
 payments(status='',orderId='',cursor?:string){return this.http.get<Page<AdminAttempt>>(`${this.base}/admin/payments`,{params:{...(status?{status}:{}),...(orderId?{orderId}:{}),...(cursor?{cursor}:{})}});}
 cases(status='',orderId='',cursor?:string){return this.http.get<Page<PaymentCase>>(`${this.base}/admin/payment-exceptions`,{params:{...(status?{status}:{}),...(orderId?{orderId}:{}),...(cursor?{cursor}:{})}});}
 case(id:string){return this.http.get<PaymentCase>(`${this.base}/admin/payment-exceptions/${id}`);}
 review(id:string,reason:string){return this.http.post<PaymentCase>(`${this.base}/admin/payment-exceptions/${id}/review`,{reason});}
 resolve(id:string,input:{reference:string;amount:number;currency:string;evidence:string;returnedAt:string}){return this.http.post<PaymentCase>(`${this.base}/admin/payment-exceptions/${id}/resolve`,input);}
 reconcile(){return this.http.post<{id:string;examined:number}>(`${this.base}/admin/payments/reconciliation-runs`,{});}
}
export function bookingError(e:unknown):string{if(e instanceof HttpErrorResponse){const code=e.error?.code;if(typeof code==='string'&&/^(ORDER_|PAYMENT_|VERIFIED_CUSTOMER|PRICE_CHANGED|TERMS_|LIMIT_|TICKETS_|INSUFFICIENT_|IDEMPOTENCY_|EVENT_UNAVAILABLE|COMMERCE_|INVALID_)/.test(code)&&typeof e.error?.detail==='string')return e.error.detail;}return errorMessage(e);}
/** Persist only a request identity, never customer/contact/payment information. */
export function requestKey(scope:string):string{try{const name=`etp-request:${scope}`;const existing=sessionStorage.getItem(name);if(existing)return existing;const key=crypto.randomUUID();sessionStorage.setItem(name,key);return key;}catch{return crypto.randomUUID();}}
export function clearRequestKey(scope:string):void{try{sessionStorage.removeItem(`etp-request:${scope}`);}catch{/* Storage may be disabled. */}}
