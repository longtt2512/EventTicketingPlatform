import type {components} from '../../api-client/src/phase4.generated';
export type Ticket=components['schemas']['Ticket'];
export type Progress=components['schemas']['Progress'];
export type CheckinEvent=components['schemas']['Event'];
export type EventAccess=components['schemas']['EventAccess'];
export type Decision=components['schemas']['Decision'];
export type LookupResult=components['schemas']['LookupResult'];
export type Admission=components['schemas']['Admission'];
export type TypeCount=components['schemas']['TypeCount'];
export type Stats=components['schemas']['Stats'];
export interface Page<T>{data:T[];hasMore:boolean;nextCursor:string|null}
export function ticketLabel(status:string):string{return ({ISSUED:'Chưa sử dụng',CHECKED_IN:'Đã check-in',VOIDED:'Đã vô hiệu'} as Record<string,string>)[status]??status;}
export function decisionCopy(d:Omit<Decision,'outcome'>&{outcome:string}):{title:string;description:string;tone:string}{
 if(d.replayed&&d.outcome==='SUCCESS')return {title:'Lượt check-in đã được ghi nhận',description:'Đây là kết quả của yêu cầu trước. Không tính là một lượt vào cửa mới; kiểm tra với người soát vé trước khi cho qua.',tone:'warning'};
 return ({SUCCESS:{title:'Check-in thành công',description:'Hệ thống đã ghi nhận một lượt vào cửa cho vé này.',tone:'success'},ALREADY_USED:{title:'Vé đã được sử dụng',description:'Không cho vào thêm lần nữa. Đối chiếu thời gian check-in bên dưới.',tone:'error'},WRONG_EVENT:{title:'Vé thuộc sự kiện khác',description:'Kiểm tra sự kiện đang chọn và vé của khách.',tone:'error'},INVALID_TICKET:{title:'Không nhận diện được vé',description:'Quét lại mã QR trên vé hoặc tra cứu mã vé chính xác.',tone:'error'},EVENT_CANCELLED:{title:'Sự kiện đã dừng nhận khách',description:'Sự kiện đã hủy hoặc đang xử lý hủy. Không cho khách qua cổng.',tone:'error'},VOIDED:{title:'Vé đã vô hiệu',description:'Vé không còn quyền vào cửa.',tone:'error'},OUTSIDE_CHECK_IN_WINDOW:{title:'Ngoài thời gian check-in',description:'Chỉ nhận khách trong khung giờ được hiển thị của sự kiện.',tone:'warning'}} as Record<string,{title:string;description:string;tone:string}>)[d.outcome]??{title:'Chưa thể xác nhận',description:'Kiểm tra lại trạng thái trước khi cho khách qua cổng.',tone:'warning'};
}
