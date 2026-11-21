import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '@event-ticketing/api-client';
import { Order } from '../../booking/src/booking-models';
import { errorMessage } from '../../auth/src/feedback';
import { AuditPage,AuditRecord,Category,CommissionChange,CommissionConfig,Configuration,Evidence,Eligibility,Page,ReportSummary,SalesPage,Settlement,Settings } from './operation-models';
@Injectable({providedIn:'root'})
export class OperationsApi {
 private readonly http=inject(HttpClient);private readonly base=API_BASE_URL;
 scope(org:string|null){return org?`/organizations/${org}`:'/admin';}
 summary(org:string|null,params:Record<string,string>){return this.http.get<ReportSummary>(this.base+this.scope(org)+'/reports/summary',{params});}
 sales(org:string|null,params:Record<string,string>){return this.http.get<SalesPage>(this.base+this.scope(org)+'/reports/sales',{params});}
 csv(org:string|null,params:Record<string,string>){return this.http.get(this.base+this.scope(org)+'/reports/sales.csv',{params,responseType:'blob'});}
 settlements(org:string|null,status='',cursor?:string){return this.http.get<Page<Settlement>>(this.base+this.scope(org)+'/settlements',{params:{...(status?{status}:{}),...(cursor?{cursor}:{})}});}
 settlement(org:string|null,id:string){return this.http.get<Settlement>(this.base+this.scope(org)+'/settlements/'+id);}
 events(org:string){return this.http.get<{id:string;name:string;status:string}[]>(`${this.base}/organizations/${org}/settlements/events`);}
 eligibility(org:string,event:string){return this.http.get<Eligibility>(`${this.base}/organizations/${org}/settlements/eligibility/${event}`);}
 request(org:string,eventId:string,key:string){return this.http.post<Settlement>(`${this.base}/organizations/${org}/settlements`,{eventId},{headers:{'Idempotency-Key':key}});}
 command(id:string,version:number,action:string,body:{reason:string}|Evidence){return this.http.post<Settlement>(`${this.base}/admin/settlements/${id}/${action}`,body,{headers:{'If-Match':String(version)}});}
 config(){return this.http.get<CommissionConfig>(this.base+'/admin/commission-config');}
 configHistory(cursor?:string){return this.http.get<Page<Configuration,number>>(this.base+'/admin/commission-config/history',{params:cursor?{cursor}:{}});}
 changeConfig(version:number,body:CommissionChange){return this.http.post<CommissionConfig>(this.base+'/admin/commission-config',body,{headers:{'If-Match':String(version)}});}
 settings(){return this.http.get<Settings>(this.base+'/admin/settings');}
 saveSetting(key:string,version:number,value:string|number,reason:string){return this.http.patch<Settings>(`${this.base}/admin/settings/${key}`,{value,reason},{headers:{'If-Match':String(version)}});}
 categories(cursor?:string){return this.http.get<Page<Category>>(this.base+'/admin/categories',{params:cursor?{cursor}:{}});}
 createCategory(name:string){return this.http.post<Category>(this.base+'/admin/categories',{name});}
 category(row:Category,reason:string){return this.http.patch<Category>(`${this.base}/admin/categories/${row.id}`,{active:!row.active,reason},{headers:{'If-Match':String(row.version)}});}
 audit(params:Record<string,string>){return this.http.get<AuditPage>(this.base+'/admin/audit-logs',{params});}
 auditDetail(source:string,id:string){return this.http.get<AuditRecord>(`${this.base}/admin/audit-logs/${source}/${id}`);}
 orders(status:string,cursor?:string){return this.http.get<Page<Order>>(this.base+'/admin/orders',{params:{...(status?{status}:{}),...(cursor?{cursor}:{})}});}
}
export function operationError(error:unknown):string {if(error instanceof HttpErrorResponse&&error.status>=400&&error.status<500&&typeof error.error?.detail==='string')return error.error.detail;return errorMessage(error);}
