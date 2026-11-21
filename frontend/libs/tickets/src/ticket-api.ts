import {inject,Injectable} from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {API_BASE_URL} from '@event-ticketing/api-client';
import {errorMessage} from '../../auth/src/feedback';
import {Ticket,Page,Progress,EventAccess,Decision,LookupResult,Admission,Stats} from './ticket-models';
@Injectable({providedIn:'root'})
export class TicketApi {
 private readonly http=inject(HttpClient);private readonly base=API_BASE_URL;
 tickets(orderId?:string,cursor?:string){return this.http.get<Page<Ticket>>(`${this.base}/tickets`,{params:{...(orderId?{orderId}:{}),...(cursor?{cursor}:{})}});}
 ticket(id:string){return this.http.get<Ticket>(`${this.base}/tickets/${id}`);}
 pdf(id:string){return this.http.get(`${this.base}/tickets/${id}/pdf`,{responseType:'blob'});}
 progress(id:string){return this.http.get<Progress>(`${this.base}/orders/${id}/tickets`);}
 events(){return this.http.get<EventAccess[]>(`${this.base}/check-in/events`);}
 event(id:string){return this.http.get<EventAccess>(`${this.base}/check-in/events/${id}`);}
 scan(event:string,credential:string,key:string){return this.http.post<Decision>(`${this.base}/check-in/events/${event}/scan`,{credential},{headers:{'Idempotency-Key':key}});}
 lookup(event:string,ticketId:string){return this.http.post<LookupResult>(`${this.base}/check-in/events/${event}/lookup`,{ticketId});}
 confirm(event:string,ticket:string,key:string){return this.http.post<Decision>(`${this.base}/check-in/events/${event}/lookup/${ticket}/confirm`,{},{headers:{'Idempotency-Key':key}});}
 history(event:string,cursor?:string){return this.http.get<Page<Admission>>(`${this.base}/check-in/events/${event}/history`,{params:cursor?{cursor}:{}});}
 stats(event:string){return this.http.get<Stats>(`${this.base}/check-in/events/${event}/stats`);}
}
export function ticketError(error:unknown):string{if(error instanceof HttpErrorResponse&&typeof error.error?.detail==='string')return error.error.detail;return errorMessage(error);}
