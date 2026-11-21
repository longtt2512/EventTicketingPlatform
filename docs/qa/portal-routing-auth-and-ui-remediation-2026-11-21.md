# Nghiệm thu sửa portal, đăng ký và UI — 21/11/2026

Đã triển khai 9 backlog được duyệt và kiểm thử trên hệ thống local đang chạy.
Giữ dữ liệu UAT hiện có; chỉ bổ sung tài khoản, tổ chức, danh mục, sự kiện và giao
dịch QA. Không reset database, push hoặc triển khai ra môi trường bên ngoài.

## Địa chỉ sử dụng

| Vai trò | Trang chủ | Đăng ký |
|---|---|---|
| Customer | http://localhost:14200/home | `/register` |
| Organizer | http://localhost:14200/org/home | `/org/register` |
| Platform Admin | http://localhost:14200/admin/home | Không tự cấp quyền Admin qua đăng ký |
| Staff | http://localhost:14200/staff/home | `/staff/register` |

Các đường dẫn gốc `/`, `/org/`, `/admin/`, `/staff/` đều vào đúng ứng dụng.
Các cổng 14201–14203 đã ngừng phục vụ. API giữ `/api/v1` tại cùng origin.
Đăng nhập Admin hiện có đã được kiểm chứng sau lần khởi động gateway cuối cùng.

## Backlog đã xử lý

| ID | Kết quả | Bằng chứng chính |
|---|---|---|
| UI-01 | Tách rõ nhãn, giá trị, hướng dẫn và nhóm thao tác ở trang duyệt hồ sơ/danh mục. Nội dung dài xuống dòng và không đè nhau. | Hồ sơ dài thật, desktop/mobile, đo khoảng cách và cỡ chữ 200%. |
| UI-02 | Nút Ngừng sử dụng/Mở lại mở hộp thoại nhập lý do. Lý do trống hoặc toàn khoảng trắng bị chặn; Hủy giữ nguyên trạng thái. | Test component và thao tác browser ngừng → mở lại trên danh mục QA. |
| UI-03 | Đồng nhất cả 7 mục điều hướng tổ chức, gồm liên kết báo cáo/đối soát; trạng thái đang chọn rõ ràng. | So sánh computed style, ảnh 1440/390px, quay lại báo cáo giữ tab Thành viên. |
| ROUTE-01 | Một listener 14200 phục vụ bốn app bằng base path; deep link, reload, asset và proxy API hoạt động. | Browser từng portal; tài nguyên không tồn tại trả 404; bốn tab đồng thời không làm mất phiên. |
| AUTH-01 | Email xác thực, gửi lại và đặt lại mật khẩu giữ portal cùng đích đến nội bộ hợp lệ. | Integration test cả bốn context; browser mở email trong context sạch và đăng nhập đúng portal. |
| AUTH-02 | Trang đăng ký và trang chủ Organizer/Staff có hướng dẫn đúng quy trình, trạng thái chờ duyệt/chờ phân công và cập nhật quyền. | Test trạng thái onboarding; đăng ký, duyệt tổ chức, cấp thành viên và check-in thực tế. |
| UI-04 | Mọi ô mật khẩu dùng điều khiển hiện/ẩn riêng: đăng nhập, đăng ký, đặt lại và đổi mật khẩu. | Unit test và browser ở cả bốn portal, giữ nguyên giá trị, toggle bằng bàn phím. |
| UI-05 | Rà soát giao diện vận hành và responsive; sửa khoảng cách form, nhãn/giá trị, nhóm thao tác và điều hướng. | Hai bộ UI review, Phase 6 và kiểm tra hồ sơ dài ở 320/390/768/1440px. |
| QA-01 | Cập nhật runner, hướng dẫn UAT và chạy lại hồi quy nghiệp vụ/UI. | Kết quả bên dưới và các báo cáo JSON. |

Staff đăng ký tại `/staff/register`, xác thực email, rồi chủ tổ chức thêm email đó
vào Thành viên với quyền **Soát vé** và phân công sự kiện. Bấm **Cập nhật quyền
truy cập** hoặc đăng nhập lại sau khi được cấp quyền. Quyền CUSTOMER nền vẫn được
giữ; chọn portal không tự cấp quyền quản trị/soát vé. Organizer gửi hồ sơ và được
Admin duyệt trước khi quản lý tổ chức. Đây là quy tắc phân quyền hiện có
IDN-R03, ORG-R03/08, không phải một chính sách quyền mới.

Email dẫn tới trang xác thực/đặt lại mật khẩu của portal ban đầu. Sau khi hoàn tất,
liên kết đăng nhập giữ đúng portal và đích đến; xác thực email không tự tạo phiên.
Context không hợp lệ và URL ngoài hệ thống bị từ chối.

Bốn portal dùng chung cookie phiên. Dùng browser profile riêng khi thử bốn tài
khoản khác nhau. Nhiều tab dùng cùng tài khoản được kiểm tra đồng thời; Web Locks
tuần tự hóa việc xoay refresh cookie để tránh tự thu hồi phiên.

## Kết quả kiểm thử

- **195 backend tests đạt**, 0 failed/error/skipped, chạy lại toàn bộ bằng
  `./backend/gradlew -p backend test --rerun-tasks`.
- **155 frontend unit tests đạt** bằng `npm --prefix frontend run test:unit`.
- **4 production builds đạt** bằng `npm --prefix frontend run build:all`.
- **1.130 điểm kiểm tra luồng/UI/API đạt** trong 13 báo cáo cuối cùng dưới đây.
  Đây là số assertion, không phải 1.130 kịch bản độc lập.
- Không có JavaScript exception hoặc HTTP 5xx trong các báo cáo có theo dõi các
  lỗi đó; bộ Phase 6 không ghi nhận phát hiện accessibility trong phạm vi đã kiểm tra.
- 10 dịch vụ báo UP; đăng nhập Admin và API có phân quyền hoạt động; cấu hình phí
  đã trở về 5%, nhắc sự kiện 24 giờ, cấu hình QR/thẻ đã khôi phục sau kiểm thử.

| Bộ kiểm tra | Đạt | Báo cáo |
|---|---:|---|
| Phase 1 — tài khoản, tổ chức, phân quyền | 88 | [JSON](../../output/playwright/phase1-result.json) |
| Phase 2 — tạo, duyệt, phát hành sự kiện | 65 | [JSON](../../output/playwright/phase2-result.json) |
| Phase 3 — đơn hàng, thanh toán | 80 | [JSON](../../output/playwright/phase3-result.json) |
| Biên giao dịch — hết hạn, callback trễ/mất | 8 | [JSON](../../output/playwright/phase3-edge-result.json) |
| Phase 4 — QR/PDF, camera decoder, check-in | 76 | [JSON](../../output/playwright/phase4-result.json) |
| Phase 5 — báo cáo, CSV, đối soát, vận hành | 87 | [JSON](../../output/playwright/phase5-result.json) |
| Khoản thu chưa ánh xạ và hoàn trả mô phỏng | 4 | [JSON](../../output/playwright/phase5-unmapped-result.json) |
| Phase 6 — responsive, accessibility, tranh chấp vé cuối | 259 | [JSON](../../output/playwright/phase6-result.json) |
| Phương thức QR/thẻ và cấu hình kết quả | 66 | [JSON](../../output/playwright/payment-methods-result.json) |
| UI review bốn workspace | 128 | [JSON](../../output/playwright/ui-review-result.json) |
| Layout/navigation regression | 65 | [JSON](../../output/playwright/mockup-ui-result.json) |
| Portal/email/password/category/concurrent tabs | 185 | [JSON](../../output/playwright/portal-remediation/result.json) |
| Hồ sơ dài và điều hướng tổ chức | 19 | [JSON](../../output/playwright/portal-remediation/review-result.json) |

[Tổng hợp assertion](../../output/playwright/portal-remediation/regression-summary.json),
[backend](../../output/playwright/portal-remediation/backend-tests.json),
[frontend và coverage](../../output/playwright/portal-remediation/frontend-unit-tests.json),
[trạng thái runtime](../../output/playwright/portal-remediation/runtime-final.json).

## Ảnh kiểm chứng

- Duyệt hồ sơ dài: [desktop](../../output/playwright/portal-remediation/admin-review-long-content-1440.png), [mobile](../../output/playwright/portal-remediation/admin-review-long-content-390.png).
- Điều hướng Organizer: [desktop](../../output/playwright/portal-remediation/organizer-uniform-navigation-1440.png), [mobile](../../output/playwright/portal-remediation/organizer-uniform-navigation-390.png).
- Danh mục: [bố cục](../../output/playwright/portal-remediation/admin-categories-1440.png), [hộp thoại lý do](../../output/playwright/portal-remediation/category-dialog-390.png).
- [Staff chưa được cấp quyền](../../output/playwright/portal-remediation/checkin-onboarding-390.png).

## Chạy lại và giới hạn

Theo [runbook local](../local-development.md) để chạy các phase theo thứ tự và
chuẩn bị fixture. Hai runner mới chạy liên tiếp:

```sh
python3 scripts/run-portal-remediation-browser.py
python3 scripts/run-portal-review-browser.py
```

Runner đầu tạo applicant mới; runner sau dùng applicant đó để gửi/duyệt hồ sơ dài.
Script/log có thông tin đăng nhập thử nghiệm nằm trong `.runtime/` được bảo vệ;
không đưa chúng vào repository. Hướng dẫn người dùng đã cập nhật tại
[kịch bản UAT](manual-uat-from-empty.md); phần “database rỗng” chỉ áp dụng sau một
lần reset riêng được cho phép, không mô tả dữ liệu sau đợt QA này.

Thanh toán/hoàn trả/đối soát là mô phỏng local, email được nhận qua MailHog. Camera
test dùng video chứa QR thật để chạy decoder; chưa kiểm chứng camera vật lý hoặc
Safari/Firefox/thiết bị di động thực. Kiểm tra bàn phím, viewport và cỡ chữ không
thay thế kiểm toán screen reader/WCAG đầy đủ.

Coverage frontend: lines 89,41%, statements 80,66%, functions 78,19%, branches
60,44%. Không tuyên bố toàn bộ chỉ số đạt 85%. Build còn cảnh báo về hai
RouterLink không dùng và ngân sách SCSS operations/payment. Detector nguồn trên
15 file UI đã sửa trả về 0 phát hiện; đây là kiểm tra tĩnh, bổ sung cho quan sát
ảnh thực tế, không phải chứng nhận chất lượng giao diện tuyệt đối. Không thay đổi
CI, hook hoặc branch protection.
