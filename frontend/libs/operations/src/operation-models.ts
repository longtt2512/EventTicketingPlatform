import type { components } from '../../api-client/src/phase5.generated';
type S=components['schemas'];
export type ReportSummary=S['ReportSummary'];export type ReportRow=S['ReportRow'];export type SalesPage=S['SalesPage'];
export type Settlement=S['Settlement'];export type Eligibility=S['Eligibility'];export type Evidence=S['Evidence'];
export type Configuration=S['Configuration'];export type CommissionConfig=S['CommissionConfig'];export type CommissionChange=S['CommissionChange'];
export type Category=S['Category'];export type Settings=S['Settings'];export type Setting=S['Setting'];export type AuditRecord=S['AuditRecord'];export type AuditPage=S['AuditPage'];
export interface Page<T,C=string>{data:T[];hasMore:boolean;nextCursor:C|null;}
export const amount=(v:number|null|undefined)=>v==null?'Chưa đủ dữ liệu':new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0}).format(v);
export const percentage=(v:number|null|undefined)=>v==null?'Chưa có dữ liệu':new Intl.NumberFormat('vi-VN',{maximumFractionDigits:2}).format(v)+'%';
export const labels:Record<string,string>={PENDING:'Chờ xử lý',PROCESSING:'Đang xử lý',SETTLED:'Đã hoàn tất',FAILED:'Chắc chắn chưa trả',UNKNOWN:'Chưa rõ kết quả',NOT_STARTED:'Chưa bắt đầu',NOT_PAID:'Đã xác minh chưa trả',PAID:'Đã ghi nhận chi trả',NO_TRANSFER:'Không cần chuyển tiền',SETTLEMENT_REQUESTED:'Đã yêu cầu đối soát',SETTLEMENT_PROCESSING:'Bắt đầu xử lý',TRANSFER_EVIDENCE_RECORDED:'Đã lưu bằng chứng',SETTLEMENT_COMPLETED:'Hoàn tất đối soát',TRANSFER_NOT_PAID:'Đã xác minh chưa trả',TRANSFER_UNKNOWN:'Kết quả cần xác minh',PAYOUT_SNAPSHOT_REFRESHED:'Đã cập nhật tài khoản nhận tiền'};
export const statusLabel=(s:string)=>labels[s]??s;
