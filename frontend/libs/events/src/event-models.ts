import type { components } from '../../api-client/src/phase2.generated';
export type TicketType=components['schemas']['TicketType'];
export type EventDetail=components['schemas']['EventDetail'];
export type EventInput=components['schemas']['EventInput'];
export type TicketInput=components['schemas']['TicketInput'];
export type EventPage=components['schemas']['EventPage'];
export type EventHistory=components['schemas']['EventHistory'];
export const CATEGORIES=['Music & Concert','Sports','Conference','Workshop','Exhibition','Festival','Entertainment','Others'];
export const EVENT_LABELS:Record<string,string>={ISSUED:'Chưa sử dụng',CHECKED_IN:'Đã check-in',VOIDED:'Đã vô hiệu',DRAFT:'Bản nháp',PENDING_REVIEW:'Chờ kiểm duyệt',APPROVED:'Đã duyệt',REJECTED:'Cần chỉnh sửa',PUBLISHED:'Đã công khai',ONGOING:'Đang diễn ra',COMPLETED:'Đã kết thúc',CANCELLED:'Đã hủy',NOT_STARTED:'Chưa mở bán',ON_SALE:'Đang mở bán',SOLD_OUT:'Hết vé',PAUSED:'Tạm dừng bán',CLOSED:'Đã đóng bán',UNAVAILABLE:'Không mở bán'};
export const label=(value:string)=>EVENT_LABELS[value]??value;
export const money=(value:number|null|undefined)=>value==null?'Chưa có giá vé':value===0?'Miễn phí':new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0}).format(value);
export const dateTime=(value:string|null,timezone='Asia/Ho_Chi_Minh')=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeStyle:'short',timeZone:timezone}).format(new Date(value)):'Chưa thiết lập';
/** Forms use the event timezone, independently of the browser's timezone. */
export function localTime(value:string|null,timezone='Asia/Ho_Chi_Minh'):string {
 if(!value)return '';const parts=new Intl.DateTimeFormat('sv-SE',{timeZone:timezone,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date(value));return parts.replace(' ','T');
}
export function utcTime(value:string,timezone='Asia/Ho_Chi_Minh'):string|null {
 if(!value)return null;
 const desired=Date.parse(value+':00Z');let candidate=desired;
 for(let i=0;i<3;i++){
  const rendered=localTime(new Date(candidate).toISOString(),timezone);
  const delta=desired-Date.parse(rendered+':00Z');candidate+=delta;if(delta===0)break;
 }
 if(localTime(new Date(candidate).toISOString(),timezone)!==value)throw new Error('Thời gian không tồn tại trong múi giờ sự kiện. Hãy chọn giờ khác.');
 return new Date(candidate).toISOString();
}

export interface AttendeeRecord {id:string;name:string;email:string;ticketType:string;status:string;orderId?:string|null;checkedInAt?:string|null;dataUpdatedAt?:string;}
