# Nghiệm thu một vòng nghiệp vụ qua bốn vai trò

**Kết quả: đạt luồng nghiệp vụ được yêu cầu trên local**, có các điểm UX cần cải
thiện bên dưới. Thực hiện mới ngày 21/11/2026, khoảng 11:16–11:23 giờ Việt Nam.
Không dùng kết quả regression ngày trước để tính cho lượt này.

Sự kiện xuyên suốt: **Workshop nghiệm thu 4 vai trò 20261121-1116**.
Sử dụng bốn tài khoản synthetic khác nhau, mỗi vai trò trong browser context riêng.
Mười health endpoint đều UP trước khi chạy. Stack và dữ liệu cũ được giữ nguyên.

## Kết quả theo vai trò

| Vai trò | Thao tác qua giao diện thật | Kết quả quan sát |
| --- | --- | --- |
| Organizer | Tạo nháp, tải ảnh bìa, điền mô tả/địa điểm/điều khoản/thời gian, tạo hạng vé, gửi kiểm duyệt | Hạng vé 100.000đ, quota 20, tối đa 4 vé/đơn; trạng thái chuyển chờ duyệt |
| Admin | Xem nội dung/hạng vé, ghi chú và xác nhận phê duyệt | Sự kiện được duyệt; Organizer tiếp tục công khai |
| Organizer | Công khai và phân công nhân viên | Khách tìm thấy sự kiện; Staff thấy đúng phân công trong cửa sổ check-in |
| Customer | Tìm sự kiện, chọn 2 vé, đăng nhập, xác nhận điều khoản, thanh toán QR local | Một đơn CONFIRMED, tổng 200.000đ; phát đúng 2 vé có ID riêng; mở QR và tải PDF A4 một trang |
| Staff | Quét QR vé đầu, thử quét trùng; tra cứu và xác nhận vé thứ hai | Quét đầu thành công, quét trùng bị từ chối; tổng đúng 2 lượt, một QR và một thủ công |
| Customer | Làm mới trạng thái vé sau check-in | Vé đã dùng và QR được ẩn |
| Organizer | Xem người tham dự, báo cáo và tải CSV | 2 vé đã check-in, tham dự 100%, bán 2/20 = 10%; số tiền khớp đơn |
| Organizer / Admin | Kiểm tra điều kiện, kết thúc fixture, yêu cầu đối soát, nhập/xác nhận bằng chứng mô phỏng | Trước khi kết thúc bị chặn; sau kết thúc một batch SETTLED / PAID mô phỏng; tải lại không còn nút trả lần hai |
| Organizer | Xem kết quả đối soát và tải lại báo cáo | Đối soát hoàn tất 190.000đ, lịch sử và bằng chứng hiển thị; doanh thu/vé/check-in giữ nguyên |

## Số liệu đối chiếu cuối

| Chỉ tiêu | Giá trị |
| --- | ---: |
| Đơn xác nhận | 1 |
| Vé trả phí đã phát hành | 2 |
| Lượt check-in duy nhất | 2 |
| Doanh thu / khoản thu | 200.000đ |
| Phí nền tảng, snapshot 5% | 10.000đ |
| Phí gateway đã đối soát local | 0đ |
| Doanh thu thuần | 190.000đ |
| Đối soát đã hoàn tất | 190.000đ |
| Khoản hoàn / ngoại lệ còn mở | 0đ / 0 |

CSV tải qua UI có đúng một dòng của cùng đơn/sự kiện, cùng thời điểm snapshot
hiển thị trên báo cáo và đúng các số tiền trên. MailHog có hai email gắn với đơn
mới của khách. Không suy ra email đối soát Organizer đã được kiểm chứng từ lượt
tìm theo tên sự kiện; phần đó không dùng làm điều kiện đạt của kịch bản.

## Định danh để xem lại

- Sự kiện: `6076cf37-11fb-41d3-b593-248f61da3242`.
- Tổ chức: `3610577e-e053-4547-83b3-c246624d5a33`.
- Đơn: `2ab8c4a4-60f7-4084-a814-325acd73ddd1`.
- Vé QR: `4c1f5941-b2f3-3b0c-adfb-e9b9a66af10f`.
- Vé thủ công: `de6aca1a-f41c-3062-b934-3239026e1641`.
- Batch: `98bcab26-3be8-4438-9cfd-1749b2e941a4`.
- Bằng chứng: `MOCK-SETTLEMENT-UAT-1795234926037`.

[Đơn của Customer](http://localhost:14200/account/orders/2ab8c4a4-60f7-4084-a814-325acd73ddd1),
[sự kiện của Organizer](http://localhost:14201/organizations/3610577e-e053-4547-83b3-c246624d5a33/events/6076cf37-11fb-41d3-b593-248f61da3242),
[báo cáo Organizer](http://localhost:14201/organizations/3610577e-e053-4547-83b3-c246624d5a33/analytics),
[đối soát Admin](http://localhost:14202/settlements),
[thống kê Staff](http://localhost:14203/check-in/events/6076cf37-11fb-41d3-b593-248f61da3242/stats).
Cần đăng nhập đúng tài khoản fixture; credential chỉ nằm trong `.runtime/`.

## Nhận xét thao tác và nội dung

Các trạng thái chính dễ phân biệt: xác nhận đơn → phát hành vé; check-in thành
công xanh và vé trùng đỏ; đối soát ghi rõ mô phỏng và số tiền ở hộp xác nhận.
Đã xem ảnh desktop 1440px và mobile 390px của các bước chính. Không phát hiện
tràn ngang toàn trang trong những màn đã đo; bảng dữ liệu dùng cuộn ngang riêng.

| Mức | Quan sát | Ảnh hưởng / hướng cải thiện |
| --- | --- | --- |
| P2 | Mở bản nháp vừa tạo xuất hiện cảnh báo thay đổi chưa lưu. Dữ liệu nháp đã tồn tại và mở lại đúng. `events.component.ts` tự điền `eventId` cho form phân công, trong khi `hasUnsavedChanges()` tính cả lựa chọn này. | Cảnh báo dễ gây hiểu nhầm rằng sự kiện chưa được lưu. Nên phân biệt thay đổi người dùng với lựa chọn tự điền. Chưa sửa trong lượt nghiệm thu. |
| P3 | Khách đã tích điều khoản trước đăng nhập, khi trở lại số lượng vẫn là 2 nhưng phải tích lại điều khoản. UI báo lỗi rõ khi chưa tích. | Thêm một thao tác lặp. Đây là nhận xét UX, không kết luận vi phạm chính sách đồng ý điều khoản. |
| P2 | Lọc báo cáo theo sự kiện yêu cầu nhập UUID. | Khó dùng với Organizer bình thường; nên cho tìm/chọn tên sự kiện. Lượt này dùng ID của sự kiện đã tạo để đối chiếu chính xác. |
| P3 | Sau lifecycle đổi trạng thái fixture, dropdown sự kiện trên trang đối soát đang mở còn giữ nhãn PUBLISHED; thông báo tạo yêu cầu cũng còn sau khi xem batch SETTLED. | Thông tin cũ cạnh kết quả mới gây nhiễu. Kiểm tra điều kiện và số tiền vẫn đọc đúng từ server; tải lại trang lấy trạng thái mới. |

Không có lỗi chặn luồng còn tồn đọng. Lượt này chỉ nghiệm thu và ghi nhận, không
thay đổi mã nguồn sản phẩm hoặc tự quyết định lại chính sách.

## Bằng chứng

Thư mục [artifact local](../../output/playwright/uat-four-roles/) có 66 ảnh
(bao gồm ảnh lúc runner dừng), JSON theo bước, PDF và hai CSV.
[Summary](../../output/playwright/uat-four-roles/summary.json) chứa hash artifact,
96 checks ở các stage hoàn tất và sáu phép đối chiếu artifact. Các stage đó không
ghi nhận JavaScript exception hoặc HTTP 5xx. Không coi đây là tổng regression toàn hệ thống.

| Bước | Ảnh / tệp |
| --- | --- |
| Organizer xem lại trước gửi | [Mobile](../../output/playwright/uat-four-roles/02-organizer-review-390.png) |
| Admin xét duyệt | [Desktop](../../output/playwright/uat-four-roles/03-admin-review-1440.png) |
| Customer xác nhận và nhận vé | [Mobile](../../output/playwright/uat-four-roles/12-order-confirmed-390.png) |
| QR thật trên vé | [Mobile](../../output/playwright/uat-four-roles/14-customer-ticket-qr-390.png) |
| Check-in thành công / trùng | [Thành công](../../output/playwright/uat-four-roles/17-staff-checkin-success-390.png), [bị chặn](../../output/playwright/uat-four-roles/18-staff-duplicate-blocked-390.png) |
| Hai lượt tham dự | [Thống kê](../../output/playwright/uat-four-roles/21-staff-stats-390.png) |
| Hoàn tất đối soát | [Organizer](../../output/playwright/uat-four-roles/31-organizer-settled-390.png) |
| Báo cáo cuối | [Desktop](../../output/playwright/uat-four-roles/32-final-revenue-1440.png), [CSV](../../output/playwright/uat-four-roles/revenue-final.csv) |

Các lần runner dừng được giữ lại: cảnh báo điều hướng sau tạo nháp; chưa tích lại
điều khoản sau đăng nhập nên không phát sinh request đặt vé; và bắt nhầm response
báo cáo khởi tạo chưa lọc. Đã tiếp tục trên chính sự kiện/đơn này, chờ response đã
lọc và đối chiếu UI/API/CSV. Không bỏ assertion, không tạo đơn hoặc chi trả lại.

## Giới hạn và trạng thái bàn giao

- Chỉ provider thanh toán/chi trả là mô phỏng local; đơn, vé, QR, check-in, báo cáo
  và batch được lưu thật trong database validation. Không thu/chuyển tiền thật.
- QR được đưa vào bộ giải mã đang chạy bằng video tổng hợp chứa ảnh QR thực.
  Chưa thử camera vật lý, thiết bị thật, browser engine khác hoặc screen reader.
- Để hoàn tất cùng phiên, sau check-in đã chỉnh **chỉ giờ bắt đầu/kết thúc của
  đúng một event synthetic** theo phương pháp runbook Phase 5. Giờ ban đầu
  12:46–15:16 được đổi thành 10:21:40–11:21:39; lifecycle thực chuyển COMPLETED.
  Không sửa trực tiếp status, order, ticket, admission, phí, batch hoặc đồng hồ ứng dụng.
- Giữ dữ liệu để xem lại; event đã kết thúc và hai vé đã dùng. Không dùng lại QR
  này để diễn thử thành công lần hai.
- Không sửa cấu hình phí/thanh toán, không chạy lại toàn bộ native suite vì không
  đổi sản phẩm, không commit/push/deploy. Chỉ bổ sung hồ sơ nghiệm thu và artifact.
- Friction của công cụ: chờ đúng request đã lọc và trạng thái tải xong khi đọc báo
  cáo. Ghi nhận tại đây, không thay đổi Harness/runbook trong phạm vi này.
