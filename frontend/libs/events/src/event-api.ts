import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '@event-ticketing/api-client';
import { firstValueFrom } from 'rxjs';
import { errorMessage } from '../../auth/src/feedback';
import { EventDetail, EventInput, EventHistory, EventPage, TicketInput, AttendeeRecord } from './event-models';

@Injectable({providedIn:'root'})
export class EventApi {
 private readonly http=inject(HttpClient);private readonly base=API_BASE_URL;
 categories(){return this.http.get<string[]>(`${this.base}/discover/categories`);}
 private path(org:string,id:string){return `${this.base}/organizations/${org}/events/${id}`;}
 detail(org:string,id:string){return this.http.get<EventDetail>(this.path(org,id));}
 update(org:string,id:string,version:number,input:EventInput){return this.http.patch<EventDetail>(this.path(org,id),input,{headers:{'If-Match':String(version)}});}
 command(org:string,id:string,version:number,action:string,reason=''){return this.http.post<EventDetail>(`${this.path(org,id)}/${action}`,{reason},{headers:{'If-Match':String(version)}});}
 ticket(org:string,id:string,version:number,input:TicketInput,ticket?:string){const url=`${this.path(org,id)}/ticket-types`;return ticket?this.http.patch<EventDetail>(`${url}/${ticket}`,input,{headers:{'If-Match':String(version)}}):this.http.post<EventDetail>(url,input,{headers:{'If-Match':String(version)}});}
 pause(org:string,id:string,ticket:string,version:number,paused:boolean){return this.http.post<EventDetail>(`${this.path(org,id)}/ticket-types/${ticket}/${paused?'pause':'resume'}`,{},{headers:{'If-Match':String(version)}});}
 history(org:string,id:string){return this.http.get<EventHistory[]>(`${this.path(org,id)}/history`);}
 attendees(org:string,id:string,cursor?:string){return this.http.get<{data:AttendeeRecord[];hasMore:boolean;nextCursor:string|null}>(`${this.path(org,id)}/attendees`,{params:cursor?{cursor}:{}});}
 queue(status:string,cursor?:string){return this.http.get<EventPage>(`${this.base}/admin/events/approval`,{params:{...(status?{status}:{}),...(cursor?{cursor}:{})}});}
 review(id:string){return this.http.get<EventDetail>(`${this.base}/admin/events/${id}`);}
 moderate(id:string,version:number,action:string,reason=''){return this.http.post<EventDetail>(`${this.base}/admin/events/${id}/${action}`,{reason},{headers:{'If-Match':String(version)}});}
 feature(id:string,version:number,featured:boolean){return this.http.post<EventDetail>(`${this.base}/admin/events/${id}/feature`,{featured},{headers:{'If-Match':String(version)}});}
 search(params:Record<string,string>){return this.http.get<EventPage>(`${this.base}/discover/events`,{params});}
 publicDetail(id:string){return this.http.get<EventDetail>(`${this.base}/discover/events/${id}`);}
 media(id:string){return this.http.get<{url:string}>(`${this.base}/media/${id}/download`);}
 async upload(org:string,event:string,file:File,kind:'BANNER'|'GALLERY'):Promise<string>{
  if(!['image/png','image/jpeg'].includes(file.type)||file.size===0||file.size>5242880)throw new Error('Chọn ảnh PNG hoặc JPEG, tối đa 5 MB.');
  const digest=await crypto.subtle.digest('SHA-256',await file.arrayBuffer());const sha256=Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,'0')).join('');
  const intent=await firstValueFrom(this.http.post<{id:string;uploadUrl:string}>(`${this.base}/media/uploads`,{organizationId:org,eventId:event,kind,contentType:file.type,byteSize:file.size,sha256}));
  // Fetch intentionally bypasses the API authorization interceptor for presigned storage URLs.
  const response=await fetch(intent.uploadUrl,{method:'PUT',headers:{'Content-Type':file.type},body:file});
  if(!response.ok)throw new Error('Chưa tải được ảnh. Kiểm tra mạng và chọn lại ảnh để thử lại.');
  const result=await firstValueFrom(this.http.post<{mediaId:string}>(`${this.base}/media/uploads/${intent.id}/complete`,{}));return result.mediaId;
 }
}
export function eventError(error:unknown):string {
 if(error instanceof HttpErrorResponse&&['CANCELLATION_PENDING','EVENT_VALIDATION','MEDIA_INVALID','MEDIA_EXPIRED','MEDIA_IN_USE','MEDIA_STORAGE_UNAVAILABLE','ATTENDEES_UNAVAILABLE'].includes(error.error?.code))return String(error.error.detail);
 if(error instanceof Error&&!(error instanceof HttpErrorResponse))return error.message;
 return errorMessage(error);
}
