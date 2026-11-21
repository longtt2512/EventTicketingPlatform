import type { components } from '../../api-client/src/phase3.generated';
export type BookingQuote=components['schemas']['BookingQuote'];
export type OrderItem=components['schemas']['OrderItem'];
export type Order=components['schemas']['Order'];
export type Attempt=components['schemas']['Attempt'];
export type CheckoutSession=components['schemas']['CheckoutSession'];
export type PaymentCase=components['schemas']['PaymentCase'];
export type AdminAttempt=components['schemas']['AdminAttempt'];
export type PaymentStatus=components['schemas']['PaymentStatus'];
export interface Page<T> {data:T[];hasMore:boolean;nextCursor:string|null;}
const labels:Record<string,string>={EXCEPTION_OPENED:'Đã mở ngoại lệ',REVIEW_STARTED:'Đã nhận xử lý',RETURN_RECORDED:'Đã ghi nhận khoản hoàn',PENDING_PAYMENT:'Chờ thanh toán',CONFIRMED:'Đã xác nhận',EXPIRED:'Đã hết hạn',CANCELLED:'Đã hủy',PENDING:'Đang chờ kết quả',SUCCESS:'Thanh toán thành công',FAILED:'Thanh toán thất bại',FREE:'Đơn miễn phí',NONE:'Chưa thanh toán',EXCEPTION:'Cần đối soát',REFUNDED:'Đã ghi nhận hoàn tiền',APPLIED:'Đã áp dụng vào đơn',OPEN:'Chưa xử lý',IN_REVIEW:'Đang xử lý',RESOLVED:'Đã xử lý',LATE_SUCCESS:'Thanh toán đến sau hạn',AMOUNT_MISMATCH:'Số tiền hoặc tiền tệ không khớp',DUPLICATE_COLLECTION:'Ghi nhận thu trùng',EVENT_CANCELLED:'Sự kiện đã hủy',EVENT_UNAVAILABLE:'Sự kiện không còn bán vé',UNMATCHED_TRANSACTION:'Chưa khớp đơn hàng'};
export const orderLabel=(value:string)=>labels[value]??value;
export function remainingSeconds(expiresAt:string,serverAtLoad:string,elapsedMs:number):number{return Math.max(0,Math.ceil((Date.parse(expiresAt)-Date.parse(serverAtLoad)-elapsedMs)/1000));}
export function countdown(seconds:number):string{return `${Math.floor(seconds/60).toString().padStart(2,'0')}:${(seconds%60).toString().padStart(2,'0')}`;}
export function parseSelection(value:string|null):Record<string,number>{const result:Record<string,number>={};if(!value)return result;for(const part of value.split(',').slice(0,100)){const [id,count]=part.split(':');const n=Number(count);if(/^[a-f0-9-]{36}$/i.test(id)&&Number.isSafeInteger(n)&&n>0&&n<=10000)result[id]=n;}return result;}

export function currencyAmount(value:number,currency:string):string { return new Intl.NumberFormat('vi-VN',{style:'currency',currency,maximumFractionDigits:0}).format(value); }
