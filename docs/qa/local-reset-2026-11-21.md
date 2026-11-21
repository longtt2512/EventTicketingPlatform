# Biên bản reset môi trường local ngày 21/11/2026

**Hoàn tất:** dữ liệu nghiệp vụ cũ đã được xóa khỏi stack đang sử dụng; migrations
và bootstrap đã chạy lại. Hệ thống sẵn sàng cho
[29 kịch bản tự kiểm thử](manual-uat-from-empty.md).

## Phạm vi và thay đổi

- Project Docker: `event-ticketing-validation`; PostgreSQL `localhost:15432`,
  database `event_ticketing`; frontend `14200–14203`, backend `18080–18089`.
- Dừng application writers bằng launcher sở hữu process, sao lưu, rồi tạo lại
  đúng project cùng PostgreSQL, Kafka, Redis, MinIO và MailHog. Kafka cũ được bỏ
  để không phát lại event và tái tạo projection cũ.
- Năm volume dữ liệu mới không trùng volume trước reset. 23 container ngoài
  project vẫn còn; không reset project `event-ticketing-local` cổng 5432.
- Giữ credential/encryption key, source và bản build. Admin bootstrap hiện tại
  `admin@test.com` được tạo lại; mật khẩu là giá trị user đã chọn, không ghi vào
  tài liệu này. Email verified, trạng thái ACTIVE, có quyền ADMIN.
- Sáu file fixture cũ ở `.runtime/phase1/*fixtures.json` được chuyển vào backup,
  tránh chạy tiếp với ID/tài khoản không còn tồn tại. Artifact nghiệm thu trước
  reset là lịch sử, các link dữ liệu cũ không còn hiệu lực.

## Trạng thái đã kiểm chứng

| Hạng mục | Sau reset |
| --- | --- |
| Tài khoản người dùng | 1: `admin@test.com` |
| Tổ chức / hồ sơ / thành viên / phân công / tài khoản nhận tiền | 0 |
| Sự kiện / hạng vé / đơn hàng | 0 |
| Giao dịch / khoản thu / khoản hoàn / ngoại lệ | 0 |
| Vé / admission / nghĩa vụ phát vé | 0 |
| Batch đối soát / confirmed order trong Finance | 0 |
| Projection sự kiện / người tham dự | 0 |
| Doanh thu / phí / thuần / đối soát / check-in trong báo cáo | 0 |
| MailHog / Redis | 0 email / 0 key |
| MinIO | Không còn bucket dữ liệu người dùng; chỉ thư mục hệ thống `.minio.sys` |
| Danh mục / phí / phương thức thanh toán | 8 danh mục, phí nền tảng 5%, QR và thẻ mặc định |
| Backend health | 10/10 UP |
| Browser smoke cuối | 23 checks đạt, không JS exception hoặc HTTP 5xx |

Không phải mọi bảng kỹ thuật đều có 0 dòng: Flyway history, cấu hình, cursor đồng
bộ và sổ tổng hợp tài chính rỗng được khởi tạo lại. Một report entity PAYMENT
có `ledgerScope=UNMAPPED` được worker mới tạo với transactions/returns/attempts/
openExceptions đều là mảng rỗng; đã kiểm tra riêng để phân biệt với dữ liệu cũ.
Đăng nhập/xem dashboard khi kiểm chứng tạo refresh session và snapshot báo cáo
kỹ thuật. Không tạo Organizer/Customer/Staff, sự kiện, đơn hay vé demo sau reset.

Database được kiểm tra hai lần sau khi workers hoạt động; các bảng nghiệp vụ
vẫn rỗng. Browser đăng nhập đúng Admin, kiểm tra danh sách chỉ một user, dashboard
rỗng, danh sách duyệt/đối soát rỗng, discovery khách rỗng và màn đăng nhập bốn app.
Đã đăng xuất và đóng phiên browser của lượt smoke; stack được giữ chạy cho user.

## Bằng chứng và backup

- [Database sau khởi tạo](../../output/playwright/reset-local/data-initial.json)
  và [đối chiếu cuối](../../output/playwright/reset-local/data-final.json): số dòng
  từng bảng, các seed kỹ thuật, health và cấu hình.
- [Browser smoke](../../output/playwright/reset-local/smoke.json),
  [dashboard rỗng](../../output/playwright/reset-local/admin-empty-home.png),
  [Admin duy nhất](../../output/playwright/reset-local/admin-only-user.png),
  [discovery rỗng](../../output/playwright/reset-local/customer-empty-discovery.png).
- Backup ở `.runtime/before-reset-20261121-113817/`, khoảng 118,5 MB cho dump và
  tar volume; có `manifest.json` chứa checksum, `database.dump`, roles, các volume
  dừng nhất quán, credential và 459 email thử đã lưu. Quyền thư mục 0700.
  `pg_restore --list` đọc được custom dump; tar đã liệt kê được. Chưa diễn tập
  phục hồi toàn stack, không tuyên bố đã kiểm chứng rollback end-to-end.

Các điểm dừng của công cụ đều được xử lý trước bàn giao: MailHog phân trang khi
sao lưu; so sánh ID Docker ngắn/dài khi kiểm tra container ngoài scope; phân biệt
report entity seed rỗng; và runner dùng sai câu empty-state discovery. Runner
cuối dùng đúng heading thực tế và chạy lại đủ 23 checks. Không sửa code sản phẩm,
không bỏ kiểm tra dữ liệu nghiệp vụ rỗng hoặc reset lần hai để che lỗi.

## Bắt đầu kiểm thử

1. Đóng/tải lại các tab đăng nhập cũ; dùng profile riêng cho từng vai trò.
2. Mở [Admin](http://localhost:14202/login), đăng nhập `admin@test.com` bằng mật
   khẩu đã chọn trong cuộc trò chuyện.
3. Làm [kịch bản từ hệ thống trống](manual-uat-from-empty.md), ghi vào
   [phiếu kết quả](manual-uat-results.md). Các ca thủ công đều để CHƯA CHẠY.

Không thay đổi CI, publish, deploy hoặc commit/push. Phạm vi xác minh là reset
và smoke local; không chạy lại toàn bộ regression vì sẽ nạp dữ liệu thử mới.
