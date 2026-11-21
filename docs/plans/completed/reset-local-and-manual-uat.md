# Execution Plan: Reset local và bàn giao kịch bản kiểm thử thủ công

Date: 2026-11-21

## Status

Completed — 2026-11-21

## Outcome and authority

User yêu cầu xóa toàn bộ database sau khi đã chọn Admin bootstrap
`admin@test.com`, rồi viết kịch bản để tự test lại. Scope là stack validation
đang dùng trên localhost 14200–14203 / PostgreSQL 15432, theo
`docs/local-development.md`. `event-ticketing-local` cổng 5432 là stack khác.

## Approach

1. Xác minh ownership, lưu snapshot trước reset, dừng application writers.
2. Sao lưu PostgreSQL và trạng thái storage/broker local vào ignored `.runtime/`;
   giữ credential, encryption key và source/build hiện hành.
3. Làm mới đúng dependency project validation để DB, Kafka, cache, mail và object
   storage nhất quán; restart launcher để chạy migrations/bootstrap Admin.
4. Kiểm chứng 10 health endpoint, bốn UI, một Admin và dữ liệu nghiệp vụ rỗng,
   kiểm tra lại sau khi worker/broker chạy để loại trừ dữ liệu cũ quay lại.
5. Bàn giao kịch bản tiếng Việt: chuẩn bị, bốn vai trò, luồng chính và ca lỗi,
   dữ liệu mẫu, số liệu mong đợi, cách ghi kết quả. Không tự tạo fixture nghiệp vụ.

## Risks and recovery

- Thao tác xóa đã được user yêu cầu rõ; chỉ project được xác minh ở trên.
- Backup phải hoàn tất và dump đọc được trước khi xóa. Credential chứa khóa giải
  mã giữ riêng, quyền 0600/0700. Không đưa mật khẩu vào source/docs mới.
- Dừng writer trước backup; không để Kafka cũ nạp lại projection sau xóa DB.
- Dữ liệu benchmark/fixture cũ sẽ không còn hợp lệ; artifact cũ chỉ là lịch sử.
- Nếu startup lỗi, giữ backup và checkpoint; không reset lặp để che lỗi migration.
- Không push/deploy, không đổi business rules hoặc CI/Harness.

## Progress

- [x] Xác minh target, backup và khả năng đọc dump.
- [x] Reset dependency project và khởi tạo lại schema/config/Admin.
- [x] Chứng minh trạng thái sạch qua database/API/UI và sau worker chạy.
- [x] Hoàn thiện kịch bản và kiểm tra liên kết/tên thao tác.

## Validation and result

Đạt. Kết quả reset là bằng chứng mới; kịch bản thủ công bàn giao cho user
được ghi là chưa chạy, không lấy bằng chứng cũ để đánh dấu đã đạt.

Backup hoàn tất ở `.runtime/before-reset-20261121-113817/`: custom PostgreSQL
dump đọc được bằng pg_restore --list, roles, credentials, MailHog đầy đủ qua
pagination, và tar các volume sau khi dừng dependency (PostgreSQL ~87 MB,
Kafka ~22 MB, MinIO ~7 MB, Redis). Manifest có SHA-256. Bản backup MailHog đầu
tiên chưa hoàn tất do giới hạn pagination; lượt sau lấy đủ từng trang rồi mới
cho phép reset. Không bỏ qua bước backup.

Dependencies đã được tạo lại. Kiểm tra container ngoài scope ban đầu dừng do
so sánh ID Docker ngắn/dài; đã chuẩn hóa về 12 ký tự và xác minh container ngoài
project vẫn còn. Tiếp tục archive fixture/start, không chạy reset lần hai.

Kết quả cuối: 1 Admin active/verified, các bảng nghiệp vụ rỗng; health 10/10 UP,
23 browser smoke checks đạt, không JS exception/5xx. Tách seed ledger UNMAPPED
rỗng khỏi dữ liệu nghiệp vụ; kiểm tra arrays rỗng và kiểm tra DB lần hai sau worker.
MinIO chỉ có thư mục hệ thống, MailHog/Redis rỗng, năm data volume đều mới.

Bàn giao `docs/qa/manual-uat-from-empty.md` (29 ca),
`docs/qa/manual-uat-results.md` (phiếu trống) và
`docs/qa/local-reset-2026-11-21.md` (bằng chứng reset/backup/giới hạn).
Giữ stack chạy, đóng browser smoke sau logout. Không tạo dữ liệu nghiệp vụ demo.
