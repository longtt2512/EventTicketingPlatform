# Accepted local Phase 5 platform operations policy

Date: 2026-11-20
Status: Accepted by repository owner in conversation

The owner explicitly selected “Áp dụng toàn bộ gói P1–P3 (Khuyến nghị)”
after reviewing the Phase 5 proposal. The following policies are accepted
for local implementation and validation, without real transfers, push or deploy.

### P1. Phí, lịch sử và ghi nhận hoàn tiền

- Seed phí nền tảng **5%** cho đơn tạo mới sau khi bật Phase 5. Admin có thể
  đổi sang phần trăm hoặc số tiền cố định/vé; không kết hợp hai kiểu. Phần
  trăm có độ chính xác 0,01%, phạm vi 0–100%; phí cố định là số nguyên VND ≥0.
- Organizer chịu phí; khách vẫn trả đúng tổng giá vé. Đóng băng cấu hình ở
  lúc tạo đơn, chốt phí khi đơn CONFIRMED. Làm tròn HALF_UP trên từng vé rồi
  nhân số lượng; phí không vượt giá vé; vé miễn phí không chịu phí.
- Cấu hình mới chỉ có hiệu lực từ hiện tại/tương lai, không sửa ngược lịch
  sử và không chồng khoảng hiệu lực. Đơn đang giữ vé giữ snapshot cũ.
- Đơn cũ chưa có snapshot vẫn xuất hiện trong số vé/GMV lịch sử. Hiển thị
  phí và net là **chưa đủ dữ liệu**, không tính bằng 5%, không đổi thành 0,
  không cho đối soát; xử lý lịch sử cần quyết định riêng, không reset dữ liệu.
- Local provider tiếp tục mô phỏng. Phí gateway 0 VND phải được ghi rõ là
  kết quả đối soát của provider local cùng reference; thiếu kết quả vẫn là
  unknown. Không dùng 0 để thay dữ liệu thiếu hoặc coi là phí ngân hàng thật.
- Giữ nguyên snapshot gốc khi hoàn tiền. Hoàn từng phần được báo cáo theo
  từng bằng chứng và vẫn giữ nghĩa vụ còn lại; chỉ khi hoàn đủ khoản thu
  **đã áp dụng cho đơn confirmed** mới ghi một điều chỉnh đảo toàn bộ phí
  nền tảng. Hoàn khoản thu thừa/đến muộn không đảo phí của giao dịch hợp lệ.
  Phí gateway chỉ được đảo nếu có bằng chứng hoàn phí tương ứng. Số phí
  được hưởng của đơn đang hoàn một phần phải thể hiện là pending.
- Không bổ sung chargeback, tùy ý sửa số dư, hay self-service refund. Sự
  kiện CANCELLED không đi vào đối soát thông thường dù đã hoàn xong.

### P2. Đối soát local và thay đổi tài khoản nhận tiền

- Một batch cho toàn bộ doanh thu đủ điều kiện của một event; không payout
  từng phần. Event phải COMPLETED, dữ liệu đầy đủ, phí gateway đã đối soát,
  không còn nghĩa vụ/ngoại lệ chưa xử lý; tổ chức và người thao tác đủ quyền.
- Giữ trạng thái PRD: PENDING → PROCESSING → SETTLED hoặc FAILED. Retry
  FAILED trong cùng batch, chỉ khi có bằng chứng lần trước chưa chuyển tiền.
  UNKNOWN vẫn chặn thao tác chuyển lại; timeout không đồng nghĩa FAILED.
- Local ghi nhận bằng chứng mô phỏng có mã `MOCK-SETTLEMENT-…`, ngày, số tiền,
  tài khoản snapshot và nội dung xác minh. Không kết nối/chuyển tiền ngân hàng.
  Chỉ ADMIN ghi kết quả; không bổ sung quy trình hai người duyệt ở bản local.
- Net bằng 0 được đóng bằng quyết định **không cần chuyển tiền**, có lý do
  và audit; không tạo reference chuyển tiền giả. Net âm chặn chi trả để xử lý.
- Tài khoản nhận tiền giữ ID/phiên bản đã xác minh. Nếu thay đổi, dừng xử lý;
  chỉ cho cập nhật snapshot có audit trong cùng batch trước lần chuyển đầu
  hoặc sau bằng chứng chắc chắn chưa trả. Trường hợp UNKNOWN/đã trả không
  được đổi tài khoản để thử chuyển lại. Mỗi lần tiếp tục đều kiểm tra lại
  quyền, nghĩa vụ, số tiền và phiên bản tài khoản hiện hành.
- Nếu đã có dòng tiền nhưng điều kiện thay đổi, ghi nhận sự thật vào ngoại
  lệ tài chính; không giấu dòng tiền, không settle sai và không trả lần hai.

### P3. Báo cáo và vận hành

- Múi giờ mặc định Asia/Ho_Chi_Minh; lưu UTC và lọc `[from,to)`. Doanh thu/vé
  dựa trên confirmedAt; thu/hoàn tiền dựa thời điểm bằng chứng; chi trả dựa
  thời điểm trả; check-in dựa checkedInAt. Metadata chỉ rõ date basis cho
  từng nhóm số liệu; không lấy một mốc thời gian áp cho mọi nhóm.
- Sell-through = vé confirmed / tổng quota các hạng vé; capacity sự kiện
  hiển thị riêng. Đây là lựa chọn cần chốt vì UI §8 ghi capacity, còn BRS
  RPT-R03 đề xuất tổng quota. Mẫu số 0 hiển thị N/A.
- Check-in rate dùng số admission duy nhất còn thuộc quần thể đủ điều kiện
  trên cùng snapshot / số vé đủ điều kiện. Event đã hủy có lịch sử riêng,
  tỷ lệ tham dự hiện tại là N/A. Payment success rate dùng attempt có kết quả
  SUCCESS/FAILED theo thời điểm kết quả cuối, giữ lịch sử thành công trước
  refund; loại pending/unknown và đơn miễn phí.
- KPI, biểu đồ và CSV do server tính trên toàn bộ phạm vi; có thời điểm cập
  nhật và cùng bộ lọc. Phiên export giữ cùng snapshot/as-of với dashboard
  hoặc báo rõ đã có phiên dữ liệu mới. Không hiện số mẫu thay dữ liệu thật.
- Danh mục đang được dùng chỉ ngừng cho lựa chọn mới, không xóa lịch sử.
  Audit tổng hợp cho các hành động moderation, permission, cấu hình và tài
  chính từ thời điểm triển khai; không bịa lịch sử thiếu trường dữ liệu.
- Settings chỉ cho sửa các key đã chốt: `reminderLeadHours` (số nguyên
  1–168, mặc định 24) và `supportEmail` (email hợp lệ hoặc để trống). Các
  mốc giữ vé/check-in đã accepted hiển thị read-only; không đưa secrets lên UI.
- Reminder gửi một lần cho mỗi event/người nhận đủ điều kiện. Mua vé trong
  cửa sổ nhắc được gửi ở lượt xử lý tiếp theo trước startAt. Đổi cấu hình
  không gửi lại reminder đã hoàn tất; event hủy/vé voided không được nhắc.
  Nội dung theo locale được hỗ trợ, fallback tiếng Việt. Email mới dùng
  obligation/outbox và cơ chế retry hiện có; không tự đổi chính sách retry cũ.


Authority: PRD §§6.10–6.13, BRS §§12–15, SDD §§4/6.4/7 and the
explicit owner selection above. Existing decisions 0001–0004 remain in force.
