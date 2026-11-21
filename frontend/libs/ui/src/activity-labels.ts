/** User-facing vocabulary for persisted activity codes; stored evidence stays intact. */
export const ACTIVITY_LABELS: Record<string, string> = {
  ACCOUNT_LOCKED:'Khóa tài khoản', ACCOUNT_UNLOCKED:'Mở khóa tài khoản',
  APPLICATION_APPROVED:'Phê duyệt hồ sơ tổ chức', APPLICATION_REJECTED:'Từ chối hồ sơ tổ chức',
  APPLICATION_RESUBMITTED:'Gửi lại hồ sơ tổ chức', APPLICATION_SUBMITTED:'Gửi hồ sơ tổ chức', APPLICATION_UPDATED:'Cập nhật hồ sơ tổ chức',
  ORGANIZATION_UPDATED:'Cập nhật thông tin tổ chức', ORGANIZATION_SUSPENDED:'Đình chỉ tổ chức', ORGANIZATION_RESTORED:'Khôi phục tổ chức',
  MEMBER_ADDED:'Thêm thành viên', MEMBER_REVOKED:'Thu hồi quyền thành viên',
  STAFF_ASSIGNED:'Phân công nhân viên soát vé', STAFF_REVOKED:'Thu hồi phân công soát vé',
  PAYOUT_SUBMITTED:'Gửi tài khoản nhận tiền để xác minh', PAYOUT_APPROVED:'Xác minh tài khoản nhận tiền',
  PAYOUT_REJECTED:'Từ chối tài khoản nhận tiền', PAYOUT_AUTHORIZED:'Xác nhận quyền nhận tiền',
  CREATED:'Tạo sự kiện', UPDATED:'Cập nhật sự kiện', PENDING_REVIEW:'Gửi sự kiện để kiểm duyệt',
  APPROVED:'Phê duyệt sự kiện', REJECTED:'Yêu cầu chỉnh sửa sự kiện', PUBLISHED:'Công khai sự kiện',
  ONGOING:'Bắt đầu sự kiện', COMPLETED:'Kết thúc sự kiện', CANCEL_REQUESTED:'Yêu cầu hủy sự kiện', EVENT_CANCELLED:'Hủy sự kiện',
  FEATURED:'Đặt sự kiện nổi bật', UNFEATURED:'Bỏ sự kiện nổi bật', TICKET_CREATED:'Tạo hạng vé', TICKET_UPDATED:'Cập nhật hạng vé',
  TICKET_PAUSED:'Tạm dừng bán hạng vé', TICKET_RESUMED:'Mở lại hạng vé',
  INVENTORY_RESERVED:'Giữ vé', INVENTORY_CONSUMED:'Xác nhận số vé đã bán', INVENTORY_RELEASED:'Trả vé về kho', INVENTORY_EXPIRED:'Kết thúc thời gian giữ vé',
  RESERVED:'Tạo đơn và giữ vé', CONFIRMED:'Xác nhận đơn hàng', CANCELLED:'Hủy', EXPIRED:'Hết hạn giữ vé',
  PAYMENT_STARTED:'Bắt đầu thanh toán', PAYMENT_METHOD_SELECTED:'Chọn phương thức thanh toán',
  PAYMENT_SUCCESS:'Thanh toán thành công', PAYMENT_FAILED:'Thanh toán thất bại', PAYMENT_REFUNDED:'Ghi nhận hoàn tiền',
  CALLBACK_REPLAY:'Nhận lại thông báo thanh toán', ORDER_RESULT:'Cập nhật kết quả đơn hàng',
  AMOUNT_MISMATCH:'Số tiền thanh toán không khớp', LATE_SUCCESS:'Nhận thanh toán sau hạn',
  EXCEPTION_OPENED:'Mở hồ sơ ngoại lệ thanh toán', REVIEW_STARTED:'Nhận xử lý ngoại lệ',
  RETURN_RECORDED:'Ghi nhận khoản hoàn', RESOLVED:'Hoàn tất xử lý ngoại lệ', MOCK_SCENARIO:'Chạy kịch bản kiểm tra thanh toán',
  CATEGORY_CREATED:'Tạo danh mục', CATEGORY_ACTIVATED:'Mở lại danh mục', CATEGORY_DEACTIVATED:'Ngừng sử dụng danh mục',
  COMMISSION_CHANGED:'Thay đổi phí nền tảng', SETTING_CHANGED:'Thay đổi cấu hình vận hành',
  SETTLEMENT_REQUESTED:'Yêu cầu đối soát', SETTLEMENT_PROCESSING:'Bắt đầu xử lý đối soát',
  SETTLEMENT_COMPLETED:'Hoàn tất đối soát', TRANSFER_EVIDENCE_RECORDED:'Lưu bằng chứng chi trả',
  TRANSFER_NOT_PAID:'Xác nhận chưa chi trả', TRANSFER_UNKNOWN:'Ghi nhận kết quả cần xác minh',
  PAYOUT_SNAPSHOT_REFRESHED:'Cập nhật tài khoản nhận tiền đã chốt', REPORT_EXPORTED:'Xuất báo cáo',
};
const VALUE_LABELS: Record<string, string> = {
  CHECKIN:'Soát vé', MANAGEMENT:'Quản lý', FINANCE:'Tài chính', ADMIN:'Quản trị viên', CUSTOMER:'Khách mua vé', ORGANIZER:'Nhà tổ chức', CHECKIN_STAFF:'Nhân viên soát vé',
  ACTIVE:'Đang hoạt động', SUSPENDED:'Đã đình chỉ', LOCKED:'Đã khóa', REVOKED:'Đã thu hồi',
  APPROVED:'Đã phê duyệt', REJECTED:'Đã từ chối', PENDING_VERIFICATION:'Chờ xác minh', PENDING_REVIEW:'Chờ kiểm duyệt',
  DRAFT:'Bản nháp', PUBLISHED:'Đã công khai', ONGOING:'Đang diễn ra', COMPLETED:'Đã kết thúc', CANCELLED:'Đã hủy',
  PENDING:'Đang chờ', PROCESSING:'Đang xử lý', SETTLED:'Đã hoàn tất', FAILED:'Thất bại', SUCCESS:'Thành công',
  PAID:'Đã ghi nhận chi trả', NOT_PAID:'Đã xác minh chưa chi trả', NO_TRANSFER:'Không cần chuyển tiền',
  UNKNOWN:'Chưa rõ kết quả', NOT_STARTED:'Chưa bắt đầu', REFUNDED:'Đã ghi nhận hoàn tiền',
  OPEN:'Chưa xử lý', IN_REVIEW:'Đang xử lý', RESOLVED:'Đã xử lý', CONFIRMED:'Đã xác nhận', EXPIRED:'Đã hết hạn',
  ISSUED:'Chưa sử dụng', CHECKED_IN:'Đã check-in', VOIDED:'Đã vô hiệu', QR:'Mã QR', CARD:'Thẻ',
  PERCENTAGE:'Theo tỷ lệ phần trăm', FIXED_PER_TICKET:'Theo từng vé', FIXED_AMOUNT:'Số tiền cố định mỗi vé', '[REDACTED]':'Đã ẩn thông tin nhạy cảm',
  APPLIED:'Đã áp dụng', DUPLICATE:'Trùng thông báo', WRONG_AMOUNT:'Số tiền không khớp', LATE_SUCCESS:'Thanh toán sau hạn',
  AMOUNT_MISMATCH:'Số tiền không khớp', DUPLICATE_SUCCESS:'Thanh toán trùng', UNMAPPED:'Chưa xác định đơn hàng',
};
const SOURCE_LABELS: Record<string,string> = {
  'admin-operations':'Vận hành nền tảng', commerce:'Sự kiện và đơn hàng', finance:'Tài chính',
  identity:'Tài khoản', organization:'Tổ chức', payment:'Thanh toán', reporting:'Báo cáo',
};
export const activityLabel = (code:string):string => ACTIVITY_LABELS[code] ?? 'Hoạt động khác';
export const activitySource = (code:string):string => SOURCE_LABELS[code] ?? 'Hệ thống';
export function activityDetail(value:string|null|undefined):string {
  if (!value) return '';
  if (VALUE_LABELS[value]) return VALUE_LABELS[value];
  if (ACTIVITY_LABELS[value]) return ACTIVITY_LABELS[value];
  if (value.startsWith('Verified destination version ')) return 'Đã xác minh tài khoản nhận tiền, phiên bản '+value.slice(29);
  if (value === 'Verified local provider receipt') return 'Đã xác minh thông báo từ cổng thanh toán';
  if (value === 'Verified duplicate/conflicting delivery retained') return 'Đã lưu thông báo trùng hoặc khác kết quả để đối chiếu';
  return /^[A-Z]+(?:_[A-Z]+)+$/.test(value) ? 'Thông tin đang được cập nhật' : value;
}
export const activityOptions=()=>Object.entries(ACTIVITY_LABELS).map(([code,label])=>({code,label})).sort((a,b)=>a.label.localeCompare(b.label,'vi'));

const FIELD_LABELS: Record<string,string> = {
  before:'Trước thay đổi', before_value:'Trước thay đổi', after:'Sau thay đổi', after_value:'Sau thay đổi',
  action:'Thao tác', detail:'Nội dung', actor_id:'Người thao tác', actorId:'Người thao tác', reason:'Lý do',
  id:'Mã bản ghi', resource_id:'Mã đối tượng', occurred_at:'Thời điểm', occurredAt:'Thời điểm',
  name:'Tên', organizationName:'Tên tổ chức', status:'Trạng thái', permission:'Quyền', roles:'Vai trò',
  value:'Giá trị', version:'Phiên bản', key:'Thiết lập', mode:'Cách tính', rateBps:'Tỷ lệ (điểm cơ bản)',
  fixedFeeVnd:'Phí cố định (₫)', effectiveAt:'Ngày hiệu lực', createdAt:'Ngày tạo', updatedAt:'Ngày cập nhật',
  eventId:'Mã sự kiện', organizationId:'Mã tổ chức', userId:'Mã người dùng', customerId:'Mã người mua',
  eventName:'Tên sự kiện', snapshot:'Thông tin đã chốt', payout:'Tài khoản nhận tiền',
  grossVnd:'Doanh thu gộp (₫)', platformFeeVnd:'Phí nền tảng (₫)', gatewayFeeVnd:'Phí cổng thanh toán (₫)',
  returnedVnd:'Đã hoàn (₫)', platformFeeReversalVnd:'Phí đã điều chỉnh (₫)', netVnd:'Số tiền thuần (₫)',
  orders:'Đơn hàng', orderId:'Mã đơn hàng', orderCount:'Số đơn', transferStatus:'Trạng thái chi trả',
  requestedAt:'Ngày yêu cầu', settledAt:'Ngày hoàn tất', decision:'Bằng chứng đã ghi nhận',
  decisionActor:'Người ghi nhận', decisionAt:'Ngày ghi nhận', evidence:'Nội dung xác minh',
  amountVnd:'Số tiền (₫)', amount:'Số tiền', currency:'Tiền tệ', reference:'Mã tham chiếu', kind:'Loại',
  pendingReason:'Nội dung chờ xác minh', decisionKey:'Mã bằng chứng', applied_payment_id:'Mã thanh toán',
  bank_name:'Ngân hàng', account_holder:'Chủ tài khoản', masked_account:'Số tài khoản đã ẩn',
  destination_id:'Mã tài khoản nhận tiền', destination_version:'Phiên bản tài khoản',
  active:'Đang sử dụng', lastDecisionError:'Nội dung cần xác minh', source:'Nguồn',
};
export function activityFields(details:unknown, prefix=''): {label:string;value:string}[] {
  if (details === null || typeof details !== 'object') return [];
  return Object.entries(details).flatMap(([key,value]) => {
    const label = [prefix,FIELD_LABELS[key] ?? 'Thông tin bổ sung'].filter(Boolean).join(' · ');
    if (value && typeof value==='object' && !Array.isArray(value)) return activityFields(value,label);
    let text=value==null?'Chưa có':Array.isArray(value)?value.map(v=>key==='roles'?activityDetail(String(v)):String(v)).join(', '):
      typeof value==='boolean'?(value?'Có':'Không'):typeof value==='number'?new Intl.NumberFormat('vi-VN').format(value):String(value);
    if (key==='action') text=activityLabel(text);
    else if(key==='source') text=activitySource(text);
    else if(/^\d{4}-\d\d-\d\dT/.test(text)&&Number.isFinite(Date.parse(text)))
      text=new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(text));
    else if(key==='key') text=({reminderLeadHours:'Thời gian nhắc sự kiện',supportEmail:'Email hỗ trợ'} as Record<string,string>)[text]??activityDetail(text);
    else if(key==='reason'&&['MEMBER_ADDED','PAYOUT_AUTHORIZED'].includes(String((details as Record<string,unknown>)['action']))) text=activityDetail(text);
    else if (['status','permission','roles','mode','kind','transferStatus'].includes(key)) text=activityDetail(text);
    return [{label,value:text}];
  });
}
