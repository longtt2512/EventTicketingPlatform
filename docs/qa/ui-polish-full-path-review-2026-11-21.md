# Rà soát và sửa UI toàn luồng — 21/11/2026

## Kết quả và phạm vi

Đã dùng Impeccable (audit + polish) và Playwright CLI trên ứng dụng thực đang chạy
tại `http://localhost:14200`, gồm Customer, Organizer, Platform Admin và Staff.
Giữ giao diện hiện có, sửa nguyên nhân ở thành phần dùng chung. Bản ghi này thay
cho nhận định chỉ dựa vào việc trang mở được: có đo hình học điều khiển, tương tác
thật, trạng thái lỗi và ảnh desktop/mobile.

Phạm vi rà soát mới: 48 màn/trạng thái, bốn độ rộng 1440/768/390/320 CSS px;
ô mật khẩu còn được kiểm tra với chữ 200%, bàn phím và trạng thái validation.
Bộ UI-review bổ sung các bước soạn sự kiện, chọn vé, thanh toán, vé/PDF và đơn hàng.
Hồi quy Phase 1–6 kiểm tra các thao tác có thay đổi dữ liệu bằng tài khoản QA.

## Các lỗi đã xử lý

| Mức | Lỗi và nguyên nhân | Cách sửa và bằng chứng |
|---|---|---|
| P1 | Icon mắt bị đẩy xuống bởi selector chung của nút submit trong form | Giới hạn selector vào nút con trực tiếp; cố định hàng grid cho icon. Độ lệch tâm trước sửa 19px, sau sửa 0px; ô nhập và icon được đo ở trạng thái thường/lỗi trên bốn portal. |
| P1 | Báo cáo/đối soát mở route riêng nên mất ngữ cảnh tổ chức | Tạo thành phần điều hướng tổ chức dùng chung, giữ tên, trạng thái, quyền và mục đang chọn. Kiểm tra qua lại Revenue → Settlement → Members và tải trực tiếp URL. |
| P1 | Lịch sử hiện mã thao tác, quyền và giờ ISO | Dịch nhãn hoạt động/quyền/nguồn; định dạng giờ Việt Nam. Admin có bộ lọc nhãn tiếng Việt và chi tiết có nhãn thay JSON. Mã định danh, số tiền và nội dung do người dùng nhập được giữ. |
| P2 | Nút tải lại xuất hiện thường trực, buộc người dùng tự cập nhật | Bỏ các nút tải lại thông thường. Cập nhật khi vào trang, quay lại tab, nối lại mạng hoặc hoàn tất thao tác; giữ bản nháp, bộ lọc chưa áp dụng và phân trang. Nút thử lại chỉ xuất hiện khi có lỗi. |
| P2 | Chữ local/mock, snapshot/batch/payout/gateway lọt vào nội dung tác vụ | Bỏ thông báo môi trường và đổi sang ngôn ngữ tác vụ. Không đổi hợp đồng mô phỏng, bằng chứng hoặc trạng thái giao dịch. |
| P2 | Nhãn/số liệu báo cáo lệch hàng, nhãn bộ lọc sát ô nhập | Sửa margin và căn baseline của danh sách số liệu; thêm khoảng cách nhãn/ô nhập; kiểm tra hình học ở bốn độ rộng. |
| P2 | Khung chi tiết lịch sử/đối soát sát bảng; thao tác sát textarea | Thêm khoảng cách đúng phạm vi trang vận hành, giữ bố cục duyệt hồ sơ hai cột. |
| P2 | Bảng đối soát rỗng vẫn rộng và cắt thông báo trên màn nhỏ | Hiển thị thông báo rỗng ngoài bảng; menu tổ chức vẫn giữ nguyên. |
| P3 | Form yêu cầu đối soát có khung lồng khung; tiêu đề cột bị bẻ giữa từ | Bỏ khung lặp và cho tiêu đề cột ngắt ở ranh giới từ. |

Các mã hoạt động chưa biết có nhãn dự phòng, thay vì hiện enum thô. Nội dung
lý do do người dùng nhập không bị dịch nhầm chỉ vì có chữ in hoa hoặc dấu gạch
dưới. Trường lý do của thao tác thêm thành viên được hiểu theo quyền đã lưu.

## Cập nhật dữ liệu và phục hồi

- Tự cập nhật bị chặn khi đang sửa form, đang xác nhận, đang gửi giao dịch, đang
  xử lý một lượt check-in chưa rõ kết quả hoặc đang xem các trang dữ liệu mở rộng.
- Thanh toán đang chờ tiếp tục kiểm tra sau 30 giây với nhịp chậm hơn; chỉ kết
  quả từ máy chủ xác nhận đơn. Kiểm thử chứng minh không phát sinh yêu cầu trả
  tiền mới và dừng đọc khi đã có kết quả.
- Vé đang phát hành và hủy sự kiện đang xử lý tiếp tục cập nhật tiến độ.
- Mất phản hồi đối soát: “Thử lại” đọc lại bằng chứng đã lưu, không tạo lần chi
  trả khác. Nút kiểm tra lại giao dịch/check-in có ý nghĩa nghiệp vụ vẫn giữ.
- Tên sự kiện/tài khoản và ghi chú QA đã lưu có thể chứa từ “local”; đây là dữ
  liệu người dùng/fixture, không phải thông báo môi trường của giao diện.

## Kiểm chứng

Kết quả trên bản build cuối; các bộ nghiệp vụ đã chạy lại trong đợt sửa này.

| Bộ kiểm tra | Kết quả |
|---|---:|
| Frontend unit (Chrome Headless) | 162 đạt |
| Build Customer / Organizer / Admin / Staff | 4/4 đạt |
| Phase 1 — tài khoản, duyệt hồ sơ, quyền, phiên đăng nhập | 88 đạt |
| Phase 2 — tạo/duyệt/công khai sự kiện, vé, khám phá | 65 đạt |
| Phase 3 — đặt vé, thanh toán, ngoại lệ, hoàn tiền | 80 đạt |
| Phase 4 — QR, PDF, check-in, mất phản hồi, thu hồi quyền | 76 đạt |
| Phase 5 — báo cáo, đối soát, danh mục, cấu hình, lịch sử | 87 đạt |
| Phase 6 — responsive, accessibility mẫu, lỗi đọc, cạnh tranh đặt vé | 259 đạt |
| UI-review các màn nghiệp vụ liên quan | 128 đạt |
| UI polish mới — 48 màn/trạng thái | 869 đạt |
| Kiểm tra bổ sung chi tiết hồ sơ và hộp xác nhận danh mục | 13 đạt |

Các bộ trình duyệt trên đạt tổng cộng **1.665 assertions**, không có lỗi JavaScript
hoặc phản hồi 5xx ngoài dự kiến.

Đã xem ảnh thực của các nhóm màn auth/profile, tổ chức, quản trị, giao dịch và
soát vé; kiểm tra lại ảnh đầy đủ của các màn người dùng báo lỗi. Kiểm tra source
bằng Impeccable detector trả về `[]`; kết quả này chỉ là kiểm tra cơ học trên
source, không thay thế kiểm tra hình ảnh và tương tác. Không cài hook/đổi CI.

## Đánh giá Impeccable trong phạm vi đã kiểm tra

| Chiều đánh giá | Điểm / 4 | Căn cứ và giới hạn |
|---|---:|---|
| Accessibility | 3 | Nhãn, bàn phím, focus, hit target icon mắt 44px, trạng thái lỗi; Phase 6 không có lỗi ở mẫu tương phản được đo. Chưa chứng nhận WCAG toàn hệ thống. |
| Performance | 3 | Bốn build đạt, giữ lazy routes; listener có cleanup và debounce, không polling toàn bộ danh sách. Còn cảnh báo ngân sách CSS thành phần; chưa đo hiệu năng tải trên thiết bị thật. |
| Theming | 2 | Giữ token/màu giao diện hiện hành; còn giá trị màu cố định trong hệ thống cũ, không đưa thêm chế độ tối ngoài yêu cầu. |
| Responsive | 3 | Bốn độ rộng, chữ 200% cho auth, bảng rộng cuộn trong vùng riêng; chưa thử thao tác cảm ứng trên thiết bị vật lý. |
| Implementation integrity | 3 | Sửa selector/component dùng chung, kiểm tra điều hướng và dữ liệu thực; không chỉ sửa tọa độ một màn. |
| Tổng | 14/20 | Tốt trong phạm vi kiểm tra; không phải cam kết không còn lỗi trên mọi thiết bị/trạng thái. |

## Bằng chứng và giới hạn

- `output/playwright/ui-polish/result.json`: từng assertion và 48 tên màn.
- `output/playwright/ui-polish/before.json`: đo lệch icon trước sửa.
- `output/playwright/ui-polish/password-before-390.png` và
  `customer-register-error-390.png`: trước/sau, có lỗi validation.
- `organizer-revenue-1440.png`, `organizer-settlements-390.png`,
  `organizer-history-390.png`, `admin-history-detail-1440.png` trong cùng thư mục.
- `output/playwright/phase1-result.json` đến `phase6-result.json`,
  `ui-review-result.json`: hồi quy nghiệp vụ.
- Có thể chạy lại `python3 scripts/run-ui-polish-browser.py` sau khi build và
  có fixture Phase 1. Kiểm tra bổ sung có kết quả tại
  `output/playwright/ui-polish/final-inspection.json`. Hướng dẫn UAT đã bỏ các nút không còn tồn tại.

Các kiểm tra trình duyệt dùng Chromium/Chrome với viewport mô phỏng, không phải
thiết bị vật lý hoặc kiểm thử đủ Safari/Firefox. Không chạy lại backend unit vì
đợt này không đổi backend. Build còn cảnh báo kích thước CSS của checkout và
operations (2,53 kB và 2,23 kB so với ngưỡng cảnh báo 2 kB); không nâng ngưỡng
để che cảnh báo. Dữ liệu UAT có sẵn được giữ; những
thao tác thêm/sửa là fixture QA. Cấu hình phí/nhắc sự kiện do Phase 5 thay đổi
được khôi phục trong finally; đọc lại API xác nhận mức phí 5% và nhắc trước 24 giờ. Không push hoặc triển khai ra môi trường ngoài.
