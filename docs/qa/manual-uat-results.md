# Phiếu ghi kết quả kiểm thử thủ công

Hướng dẫn: [kịch bản từ hệ thống trống](manual-uat-from-empty.md).
Đợt: `________` · Người chạy: `________` · Ngày: `________`
Browser / thiết bị: `________`

Chọn trạng thái: **CHƯA CHẠY / ĐẠT / LỖI / BỊ CHẶN**. Mỗi lần chạy dùng bản
sao riêng của phiếu này. Bằng chứng smoke reset của agent không thay thế kết quả
kiểm thử thủ công của bạn.

| ID | Nội dung | Trạng thái | ID dữ liệu / ảnh / ghi chú |
| --- | --- | --- | --- |
| TC-01 | Admin, dữ liệu trống | CHƯA CHẠY | |
| TC-02 | Đăng ký, xác thực tài khoản | CHƯA CHẠY | |
| TC-03 | Lỗi form, gửi lại email | CHƯA CHẠY | |
| TC-04 | Reset mật khẩu, logout | CHƯA CHẠY | |
| TC-05 | Nộp hồ sơ tổ chức | CHƯA CHẠY | |
| TC-06 | Từ chối, gửi lại, phê duyệt | CHƯA CHẠY | |
| TC-07 | Staff, tài khoản nhận tiền | CHƯA CHẠY | |
| TC-08 | Tạo sự kiện và hai hạng vé | CHƯA CHẠY | |
| TC-09 | Duyệt, công khai, phân công | CHƯA CHẠY | |
| TC-10 | Hai vé trả phí, QR payment | CHƯA CHẠY | |
| TC-11 | QR/PDF và vé miễn phí | CHƯA CHẠY | |
| TC-12 | Scan, chặn trùng, lookup | CHƯA CHẠY | Camera vật lý: CHƯA CHẠY |
| TC-13 | Báo cáo, CSV trước đối soát | CHƯA CHẠY | |
| TC-14 | Chặn đối soát sớm | CHƯA CHẠY | |
| TC-15 | Hoàn tất đối soát | CHƯA CHẠY | |
| TC-16 | Đối chiếu cuối | CHƯA CHẠY | |
| TC-17 | Nháp thiếu, quota sai | CHƯA CHẠY | |
| TC-18 | Từ chối và duyệt lại sự kiện | CHƯA CHẠY | |
| TC-19 | Thẻ thất bại, retry thành công | CHƯA CHẠY | |
| TC-20 | Hủy đơn giữ vé | CHƯA CHẠY | |
| TC-21 | Hết hạn giữ vé | CHƯA CHẠY | |
| TC-22 | Tranh vé cuối | CHƯA CHẠY | |
| TC-23 | Dừng/tiếp tục bán | CHƯA CHẠY | |
| TC-24 | Hủy sự kiện và vô hiệu vé | CHƯA CHẠY | |
| TC-25 | Phân quyền, thu hồi Staff | CHƯA CHẠY | |
| TC-26 | Vé của người khác | CHƯA CHẠY | |
| TC-27 | Camera / mất mạng | CHƯA CHẠY | |
| TC-28 | Khóa/mở khóa tài khoản | CHƯA CHẠY | |
| TC-29 | Mobile, bàn phím, dirty form | CHƯA CHẠY | |

## Định danh luồng chính

- Tổ chức: `________`
- Sự kiện: `________`
- Đơn trả phí: `________`
- Đơn miễn phí: `________`
- Vé trả phí 1 / 2: `________` / `________`
- Vé miễn phí: `________`
- Batch đối soát / reference: `________` / `________`
- Snapshot báo cáo / tệp CSV: `________`

## Đối chiếu tiền và vé

| Chỉ tiêu | Mong đợi sau TC-16 | Thực tế |
| --- | ---: | --- |
| Đơn confirmed | 2 | |
| Vé trả phí / miễn phí | 2 / 1 | |
| Check-in duy nhất | 2 | |
| Vé chưa dùng | 1 | |
| Doanh thu | 200.000đ | |
| Phí nền tảng | 10.000đ | |
| Phí gateway local | 0đ | |
| Thuần / đối soát hoàn tất | 190.000đ / 190.000đ | |

Không cộng các sự kiện phụ TC-17…TC-29 vào bảng này. Ghi ảnh và kết quả khi có
sai lệch; không sửa dữ liệu bằng SQL để khớp số mong đợi.

## Lỗi cần xử lý

| Mã lỗi | Ca | Mức ảnh hưởng | Mong đợi / thực tế | Bằng chứng |
| --- | --- | --- | --- | --- |
| | | | | |
