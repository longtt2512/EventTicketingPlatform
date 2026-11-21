# Phase 6 — nghiệm thu ứng dụng local

Ngày: 2026-11-20. Trạng thái: hoàn tất phạm vi local đã chốt; kiểm chứng ngày 2026-11-20.

## Phạm vi đã chốt

Hoàn tất đối chiếu yêu cầu, kiểm thử chức năng và đồng thời, hồi quy bốn ứng dụng,
responsive/accessibility trong môi trường local. Theo yêu cầu của chủ repository,
không thực hiện đợt phục hồi sự cố, backup/restore, audit bảo mật/dependency hoặc
đo hiệu năng. Những assertion có sẵn trong bộ hồi quy vẫn được giữ nguyên.

Ma trận [test strategy](../08-test-strategy.md) đối chiếu **13 module, 50 BRS và
22 UX scenario**, chỉ rõ bằng chứng và phần loại khỏi phạm vi. Số hàng đối chiếu
không đồng nghĩa mọi tình huống crash hay mục tiêu phi chức năng đều đã được thử.

## Thay đổi đã thực hiện

| Mức | Phát hiện và thay đổi | Bằng chứng |
| --- | --- | --- |
| P1 | Khi người khác giữ vé cuối, dòng vé đã chọn hết hàng bị khóa cả nút giảm. Cho phép giảm về 0 để người mua chọn hạng khác; tăng số lượng vẫn bị chặn. | Booking component regression và hai trình duyệt tranh vé cuối |
| P1 | Skip link dùng fragment với base href có thể rời trang hiện tại. Chuyển focus trực tiếp vào main, giữ route và dữ liệu trên trang. | Unit test và bàn phím ở bốn ứng dụng |
| P1 | Tab tại nút cuối hộp xác nhận đi ra khỏi chuỗi thao tác của dialog. Bổ sung vòng Tab/Shift+Tab; Escape vẫn hủy và trả focus về nút mở. | Unit test và dialog Cấu hình vận hành trên browser |
| P2 | Hướng dẫn gửi lại email ở màn đăng nhập chỉ 11,2 px. Tăng lên 12 px trong hệ thống giao diện hiện có. | Đọc computed style thực tế và screenshot |
| Test runner | Phase 1 kiểm tra nút phân trang trước khi dữ liệu trang đầu tới. Chờ trang đầu và trạng thái tải hoàn tất; không đổi assertion khóa/mở khóa. | Phase 1 đạt lại 88 checks |

Bổ sung bảy backend integration tests: đơn trộn vé miễn phí/trả phí, callback khi
người mua đã khóa, không hủy đơn confirmed, 12 payment facts đồng thời, giữ giá
đơn cũ sau thay giá, tỷ lệ thanh toán N/A khi mẫu số rỗng, và chặn dùng cùng mã
chi trả cho hai đối soát khác nhau. PostgreSQL thật trong Testcontainers xác minh
trạng thái/counter/outbox/reference sau thao tác hợp lệ và thao tác bị từ chối.
Thêm hai frontend tests cho lỗi chọn vé và skip link, mở rộng test focus dialog.

## Kiểm chứng native

| Lệnh / kết quả | Quan sát |
| --- | --- |
| `./backend/gradlew -p backend test bootJar` | 193 tests, 0 failure/error/skip; gồm 190 service tests và 3 platform-starter tests |
| `npm --prefix frontend run test:unit` | 148 tests passed |
| `npm --prefix frontend run build:all` | Cả bốn ứng dụng build thành công |
| Coverage frontend | Lines 88,56%; statements 79,98%; branches 59,95%; functions 76,77% |

Đã chạy toàn bộ backend bằng `--rerun-tasks` đầu đợt; sau bổ sung test chạy lại
suite bị ảnh hưởng và lệnh tổng hợp. Gradle giữ UP-TO-DATE cho phần không đổi.
Không diễn giải coverage dòng thành coverage statement/branch trên 85%.

## Hồi quy qua ứng dụng thật

| Bộ kiểm tra | Checks đạt |
| --- | ---: |
| Phase 1 — tài khoản/tổ chức | 88 |
| Phase 2 — sự kiện/tồn kho/discovery | 65 |
| Phase 3 — đặt vé/thanh toán | 80 |
| Transaction boundaries | 8 |
| Phase 4 — vé/QR/PDF/check-in | 76 |
| Cấu hình QR/thẻ | 66 |
| Phase 5 — phí/đối soát/báo cáo/admin/email | 87 |
| Dòng tiền chưa gắn sự kiện | 4 |
| Phase 6 — mixed order/race/bốn ứng dụng | 259 |
| **Tổng đợt này** | **733** |

Các bộ Phase 1–5 đã chạy mới trong đợt này. Những sửa đổi cuối ở shared UI được
kiểm chứng lại bằng unit suite, bốn bản build và luồng Phase 6. Các báo cáo cũ như
`phase5-final-render-result.json` không được cộng vào tổng của đợt này.

Phase 5 xác minh phí đơn giữ chỗ 5% và đơn mới 7%, CSV thực, đối soát có/không
chuyển tiền mô phỏng và email trong MailHog. Số obligation delivered/total ở
fixture: APPROVED 3/3, CANCELLED 2/2, REMINDER 2/2, SETTLED 2/2. Các giá trị cấu
hình phí/reminder/QR/card được khôi phục sau test; lịch sử synthetic được giữ.

Kết quả cuối: không có JavaScript exception, 5xx ngoài dự kiến hoặc contrast finding
trong tập mẫu đo được. Mười backend health endpoint đều UP, bốn URL login trả 200;
jar đang chạy khớp SHA-256 với jar từ build đã kiểm chứng.

## Giao diện và bàn phím

- 31 bề mặt thuộc Customer, Organizer, Admin, Check-in; kiểm tra reflow ở
  320/390/768/1440 px, semantic main/h1, accessible name, alt và ID duy nhất.
- Bốn ứng dụng được thử 200% root text size và skip link giữ đúng route.
  Đây là text scaling, không đồng nhất với phép thử browser-native zoom.
- Thử lỗi tải dữ liệu/retry thật ở cả bốn app, kết quả tìm kiếm rỗng và xóa lọc,
  menu mobile bằng bàn phím, reduced motion, logout xóa view riêng tư.
- Luồng Phase 6 mua một vé trả phí + hai vé miễn phí, nhận đúng ba ticket ID
  ổn định; hai browser context độc lập được đồng bộ tại HTTP reservation để
  cùng tranh vé trả phí cuối. Kiểm tra lại tồn kho tại owner service.
- Đã xem ảnh render mobile/desktop của bốn app và màn đăng nhập. Tông màu,
  typography và bố cục hiện có được giữ; không thiết kế lại sản phẩm.
- Source detector trên các file UI sửa không có findings. URL detector ban đầu
  báo cỡ chữ login đã sửa, cùng advisory một từ cuối dòng; advisory này không
  gây mất chức năng. Contrast sampler chỉ xét text ổn định trên nền đặc đơn giản;
  nền gradient/ảnh/translucency và thời điểm transition được loại và ghi rõ.
  Lần cuối có 3148 mẫu text, 1 mẫu nền phức tạp không đo; 0 contrast finding.

## Giới hạn còn lại trong nghiệm thu local

- Build còn warning import RouterLink chưa dùng và SCSS vượt ngưỡng cảnh báo
  2 kB ở operations/payment checkout. Build vẫn đạt, không nới budget.
- Không chứng nhận đầy đủ WCAG, screen reader, thiết bị camera vật lý hay mọi
  browser engine. Camera regression dùng video tổng hợp chứa QR thật trong Chromium.
- Payment/chi trả là provider mô phỏng local theo quyết định đã chấp thuận;
  projection có độ trễ và email có thể lặp ở khoảng send/commit như đã ghi nhận.
- Tất cả hạng mục loại khỏi phạm vi ở đầu báo cáo được ghi là không thực hiện,
  không được tính là đã đạt.

## Artifact và chạy lại

`output/playwright/phase6-validation-summary.json` tổng hợp số liệu và hash/mtime
của XML, logs, source đã sửa và browser reports. `phase6-result.json` chứa từng
assertion và từng surface. `phase6-*-390.png`, `phase6-*-1440.png` và
`phase6-*-text-200.png` là ảnh render. Artifact local nằm trong thư mục ignored;
báo cáo này cùng strategy/tests/scripts là tài liệu để chạy lại.

Sau các bước trong [local runbook](../local-development.md), chạy tuần tự các
browser workflow theo strategy rồi:

```sh
python3 scripts/run-phase6-browser.py
python3 scripts/summarize-phase6-validation.py
```

Không chạy đồng thời workflow thay đổi cùng account/org/config. File credential
và log browser thô được giữ trong `.runtime/` với quyền 0600. Stack local do
launcher quản lý được giữ chạy cho nghiệm thu.

Friction được ghi nhận, không sửa Harness: một số browser runner dùng trạng thái
load trang thay cho trạng thái request sau tương tác; Phase 1 và Phase 6 đã dùng
điều kiện DOM/request cụ thể tại các điểm gặp lỗi. Source detector trên Angular
templates không thay thế kiểm tra DOM và ảnh render.
