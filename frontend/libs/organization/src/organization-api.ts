import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { components } from '../../api-client/src/phase1.generated';
export type Application=components['schemas']['Application'];
export type ApplicationInput=components['schemas']['ApplicationInput'];
export type Organization=components['schemas']['Organization'];
export type Member=components['schemas']['Member'];
export type Payout=components['schemas']['Payout'];
export type EventDraft=components['schemas']['EventDraft'];
export type Assignment=components['schemas']['Assignment'];
export type Audit=components['schemas']['Audit'];
@Injectable({providedIn:'root'})
export class OrganizationApi {
  private readonly http=inject(HttpClient); private readonly base='/api/v1';
  mine(){return this.http.get<Application>(`${this.base}/organizer-applications/me`);}
  apply(body:ApplicationInput){return this.http.post<Application>(`${this.base}/organizer-applications`,body);}
  edit(id:string,body:ApplicationInput){return this.http.patch<Application>(`${this.base}/organizer-applications/${id}`,body);}
  resubmit(id:string){return this.http.post<Application>(`${this.base}/organizer-applications/${id}/resubmit`,{});}
  applications(){return this.http.get<Application[]>(`${this.base}/admin/organizer-applications`);}
  decide(id:string,approve:boolean,reason:string){return this.http.post(`${this.base}/admin/organizer-applications/${id}/${approve?'approve':'reject'}`,{reason});}
  upload(file:File){const body=new FormData();body.append('file',file);return this.http.post<{id:string}>(`${this.base}/verification-documents`,body);}
  document(id:string){return this.http.get(`${this.base}/verification-documents/${id}`,{responseType:'blob'});}
  organizations(){return this.http.get<Organization[]>(`${this.base}/organizations`);}
  organization(id:string){return this.http.get<Organization>(`${this.base}/organizations/${id}`);}
  update(id:string,body:{name:string;contactEmail:string;address:string;description:string;version:number}){return this.http.patch<Organization>(`${this.base}/organizations/${id}`,body);}
  members(id:string){return this.http.get<Member[]>(`${this.base}/organizations/${id}/members`);}
  addMember(id:string,email:string,permission:string){return this.http.post<Member[]>(`${this.base}/organizations/${id}/members`,{email,permission});}
  revokeMember(id:string,user:string){return this.http.delete<void>(`${this.base}/organizations/${id}/members/${user}`);}
  payouts(id:string){return this.http.get<Payout[]>(`${this.base}/organizations/${id}/payout-destinations`);}
  addPayout(id:string,body:{bankName:string;accountHolder:string;accountNumber:string}){return this.http.post<Payout[]>(`${this.base}/organizations/${id}/payout-destinations`,body);}
  reviewPayout(org:string,id:string,approve:boolean,reason:string){return this.http.post<Payout[]>(`${this.base}/admin/organizations/${org}/payout-destinations/${id}/${approve?'approve':'reject'}`,{reason});}
  status(id:string,suspend:boolean,reason:string){return this.http.post<Organization>(`${this.base}/admin/organizations/${id}/${suspend?'suspend':'restore'}`,{reason});}
  events(id:string){return this.http.get<EventDraft[]>(`${this.base}/organizations/${id}/events`);}
  createEvent(id:string,name:string){return this.http.post<EventDraft>(`${this.base}/organizations/${id}/events`,{name});}
  assignments(id:string){return this.http.get<Assignment[]>(`${this.base}/organizations/${id}/event-staff`);}
  assign(org:string,event:string,user:string){return this.http.put<void>(`${this.base}/organizations/${org}/events/${event}/staff/${user}`,{});}
  unassign(org:string,event:string,user:string){return this.http.delete<void>(`${this.base}/organizations/${org}/events/${event}/staff/${user}`);}
  staffEvents(){return this.http.get<EventDraft[]>(`${this.base}/staff/events`);}
  staffEvent(id:string){return this.http.get<EventDraft>(`${this.base}/staff/events/${id}`);}
  audit(id:string){return this.http.get<Audit[]>(`${this.base}/organizations/${id}/audit`);}
}
export const labels:Record<string,string>={PENDING_VERIFICATION:'Đang chờ duyệt',APPROVED:'Đã phê duyệt',REJECTED:'Đã từ chối',ACTIVE:'Đang hoạt động',SUSPENDED:'Đã đình chỉ',REVOKED:'Đã thu hồi',PENDING_REVERIFY:'Chờ xác minh',DRAFT:'Bản nháp',PENDING_REVIEW:'Chờ kiểm duyệt',PUBLISHED:'Đã công khai',ONGOING:'Đang diễn ra',COMPLETED:'Đã kết thúc',CANCELLED:'Đã hủy',MANAGEMENT:'Quản lý',FINANCE:'Tài chính',CHECKIN:'Soát vé'};
export function statusLabel(value:string){return labels[value]??value;}
export function downloadBlob(blob:Blob){const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download='verification-document';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
