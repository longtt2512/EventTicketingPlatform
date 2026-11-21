# Execution Plan: Nghiệm thu một vòng nghiệp vụ qua bốn vai trò

Date: 2026-11-21

## Status

Completed — 2026-11-21. Có bằng chứng mới xuyên suốt cùng sự kiện/đơn/batch.

## Outcome

Thao tác trên trình duyệt thật với một sự kiện xuyên suốt: Organizer tạo/gửi duyệt,
Admin phê duyệt, Organizer công khai/phân công, Customer mua/thanh toán mô phỏng/
nhận QR, Staff check-in, Organizer xem báo cáo/yêu cầu đối soát, Admin hoàn tất
đối soát mô phỏng. Lưu bằng chứng và nhận xét về thao tác/nội dung.

## Context and scope

User yêu cầu trực tiếp kịch bản này ngày 2026-11-21. Authority:
`docs/local-development.md`, accepted decisions 0001–0005, browser workflows
Phase 2/4/5. Chỉ dùng stack validation riêng và tài khoản `@example.test`.
Không thay đổi chính sách, cấu hình toàn cục, sản phẩm, CI, Git hoặc deployment.

## Approach

1. Xác minh ownership/health, đăng nhập bằng các fixture được runbook chỉ định.
2. Tạo sự kiện mới qua UI, dùng cửa sổ bán và check-in hiện hành.
3. Theo dõi cùng event/order/ticket qua bốn ứng dụng, chụp desktop/mobile.
4. Chỉ điều chỉnh giờ của sự kiện synthetic này trong validation DB theo
   runbook Phase 5; để lifecycle thực chuyển COMPLETED rồi đối soát qua UI.
5. Đối chiếu API đọc/CSV/số liệu với thao tác và lưu báo cáo nghiệm thu.

## Risks and recovery

- Dữ liệu/source hiện có được giữ nguyên; không reset/clean/stop stack đang có.
- Credential, QR gốc và script chứa credential chỉ ở ignored `.runtime/` 0600.
- Giữ lại event/order/admission/batch synthetic để người dùng xem lại.
- Ghi ID và checkpoint mỗi bước để tiếp tục, không tạo/chi trả trùng khi retry.
- Camera dùng video chứa QR thật, không chứng nhận camera vật lý.
- Đổi giờ fixture được ghi rõ; không thay application clock hoặc policy.

## Progress

- [x] Đọc workflow/runbook, kiểm tra ownership và 10 health endpoint UP.
- [x] Organizer tạo, Admin duyệt, Organizer công khai và phân công.
- [x] Customer mua/thanh toán/nhận QR/PDF.
- [x] Staff check-in; đối chiếu người tham dự và báo cáo.
- [x] Lifecycle COMPLETED và đối soát mô phỏng.
- [x] Xem ảnh thực tế, tổng hợp bằng chứng và giới hạn.

## Validation

Browser CLI trên stack hiện có, kết quả mới trong `output/playwright/uat-four-roles/`.
Đối chiếu cùng ID ở UI, API chỉ đọc và CSV tải từ UI. Không chạy lại toàn bộ
regression khi không sửa sản phẩm. Không diễn giải bằng chứng cũ thành lượt chạy mới.

## Result

Đạt: một đơn 200.000 VND, hai vé riêng, hai check-in duy nhất; quét trùng bị từ
chối. Phí 10.000 VND, net 190.000 VND, một batch SETTLED/PAID mô phỏng; báo cáo
sau đối soát và CSV khớp cùng đơn/snapshot. Bốn tài khoản khác nhau được đối chiếu.

96 checks ở các stage hoàn tất, sáu phép đối chiếu artifact; không JS exception
hoặc 5xx trong các stage đó. Giữ cả kết quả runner dừng/phục hồi và 66 ảnh.
Không sửa mã sản phẩm, không commit/push/deploy hoặc đổi global config.

Báo cáo: `docs/qa/four-role-local-acceptance-2026-11-21.md`.
Artifact summary: `output/playwright/uat-four-roles/summary.json`.
Checkpoint chứa credential chỉ ở `.runtime/uat-four-roles/`, không đưa vào Git.
Các điểm UX ghi trong báo cáo, chưa sửa. Camera dùng video QR thật; thời gian
fixture điều chỉnh đúng một event sau khi check-in theo runbook, không đổi clock.
