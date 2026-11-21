# Kịch bản tự kiểm thử từ hệ thống trống

Ngày chuẩn bị: 21/11/2026. Đây là bộ kịch bản để **bạn tự chạy**, chưa đánh dấu
đạt thay cho bạn. Bằng chứng reset/smoke được lưu riêng tại
[biên bản reset](local-reset-2026-11-21.md).

> URL đã cập nhật sau đợt sửa portal ngày 21/11/2026. TC-01 chỉ áp dụng ngay sau
> một lần reset được yêu cầu riêng. Môi trường hiện có dữ liệu UAT của bạn và các
> fixture QA mới; không mặc định kỳ vọng tất cả danh sách trống.

## 1. Chuẩn bị và cách chạy

| Vai trò / công cụ | Địa chỉ |
| --- | --- |
| Customer | http://localhost:14200 |
| Organizer | http://localhost:14200/org |
| Admin | http://localhost:14200/admin |
| Staff check-in | http://localhost:14200/staff |
| Hộp thư thử MailHog | http://localhost:18025 |

Admin khởi tạo là `admin@test.com`, dùng mật khẩu bạn đã chọn trong cuộc trò
chuyện. Các tài khoản dưới đây **chưa tồn tại**; bạn sẽ tự đăng ký ở TC-02.
Mật khẩu đăng ký/đổi mật khẩu thông thường phải dài 10–128 ký tự. Có thể tự chọn
một mật khẩu thử dùng chung cho các tài khoản mẫu, không dùng mật khẩu thật.

| Tài khoản mẫu | Họ tên | Mục đích |
| --- | --- | --- |
| `organizer@example.test` | Chủ tổ chức UAT | Nộp hồ sơ, tạo sự kiện, đối soát |
| `buyer@example.test` | Khách mua vé UAT | Mua, nhận vé, mở QR |
| `staff@example.test` | Nhân viên UAT | Được phân công soát vé |
| `outsider@example.test` | Khách ngoài UAT | Thử phân quyền, tranh vé và ca lỗi |

**Tách phiên đăng nhập:** bốn portal dùng chung `http://localhost:14200` và một phiên đăng nhập. Dùng các browser profile riêng cho từng vai trò,
hoặc đăng xuất rồi đăng nhập đúng tài khoản khi đổi vai trò. Hai tab ẩn danh
trong cùng phiên cũng có thể dùng chung cookie. Không mở liên kết xác thực email
nhầm profile: sao chép URL trong MailHog và dán vào profile của người nhận.

Sau khi được cấp quyền mới, bấm **Cập nhật quyền truy cập** ở trang chủ Organizer/Staff,
hoặc tải lại trang. Một tài khoản có thể đồng thời có quyền mua vé, tổ chức và soát vé.

Đóng hoặc tải lại các tab cũ sau reset. Dùng tên đợt, ví dụ `UAT-01`, trong mọi
tên tổ chức/sự kiện để dễ tìm. Ghi ID khi được yêu cầu; không dùng ID từ báo cáo cũ.

Tệp upload mẫu có sẵn: [ảnh PNG](../event-ticketing-assets/backgrounds/event-plain/product-design-lab.png).
Có thể dùng ảnh này làm tài liệu giả lập của tổ chức và ảnh bìa. Chỉ dùng dữ liệu
thử; không cần giấy tờ cá nhân hoặc tài khoản ngân hàng thật.

**Thứ tự đề nghị:** TC-01 → TC-02 → TC-05…TC-16 cho luồng chính. TC-03/04 và
TC-17…TC-29 là ca bổ sung; mỗi ca chỉ rõ fixture/phạm vi để không làm lệch số liệu
luồng chính. Thời gian dự kiến luồng chính 60–90 phút, có thời gian chờ sự kiện kết thúc.

## 2. Tài khoản và trạng thái trống

### TC-01 — Admin và dữ liệu sau reset · P0

1. Đăng nhập Admin bằng tài khoản khởi tạo.
2. Mở **Tài khoản người dùng**: chỉ có `admin@test.com` trước khi bạn đăng ký ai khác.
3. Mở **Quản lý tổ chức**, **Kiểm duyệt sự kiện**, **Đơn hàng**, **Đối soát sự kiện**.
4. Mở Customer → **Sự kiện** khi chưa đăng nhập.

**Mong đợi:** danh sách nghiệp vụ trống, không còn Workshop/đơn/vé của đợt trước.
Dashboard có 1 tài khoản, 0 tổ chức, 0 sự kiện; doanh thu 0. Danh mục mặc định,
phí nền tảng 5% và cấu hình QR/thẻ còn tồn tại để ứng dụng vận hành. Tỷ lệ có mẫu
số 0 có thể hiển thị N/A, không phải lỗi.

### TC-02 — Đăng ký và xác thực bốn tài khoản mẫu · P0

1. Đăng ký Organizer tại `/org/register`, Staff tại `/staff/register`; Buyer và Outsider
   tại `/register`. Nhập họ tên/email/mật khẩu và nhập lại mật khẩu.
2. Bấm **Tạo tài khoản**; trước xác thực thử đăng nhập một lần.
3. Mở MailHog, tìm đúng người nhận, mở email xác thực mới nhất; sao chép liên kết
   vào profile tương ứng rồi bấm **Xác thực email**.
4. Đăng nhập thành công. Lặp lại cho bốn tài khoản ở bảng chuẩn bị.

**Mong đợi:** trước xác thực không vào được phiên đăng nhập; sau xác thực vào
được và trở về đúng portal đã đăng ký. Staff thấy hướng dẫn chờ cấp quyền/phân công,
Organizer thấy hướng dẫn gửi hồ sơ. Người đăng ký chưa tự có quyền Admin hoặc tổ chức. Kết thúc có 5 tài khoản
gồm Admin, không phải 4 Admin.

### TC-03 — Kiểm tra form và gửi lại email · P1

Fixture riêng: `resend-uat@example.test`, không dùng để mua vé luồng chính.

1. Thử email sai định dạng, mật khẩu dưới 10 ký tự, nhập lại mật khẩu khác nhau.
2. Sửa hợp lệ và đăng ký; chưa mở email xác thực đầu tiên.
3. Mở **Gửi lại liên kết**, nhập email, bấm **Gửi lại email xác thực**.
4. Quan sát cooldown 60 giây; gửi lại sau khi đủ thời gian nếu lần đầu bị chặn
   bởi email vừa đăng ký. Mở thư mới nhất và xác thực.
5. Mở lại liên kết đã dùng và thử xác thực lần nữa.

**Mong đợi:** lỗi form ở đúng trường, dữ liệu nhập còn; không spam được nút gửi.
Sau cooldown có thư mới; token đã dùng không xác thực lại. Thông báo gửi lại
không tiết lộ một email bất kỳ có tài khoản hay không.

### TC-04 — Quên mật khẩu và đăng xuất · P1

Fixture: tài khoản `outsider@example.test`; ghi lại mật khẩu mới để dùng ca sau.

1. Ở màn đăng nhập bấm **Quên mật khẩu?**, gửi yêu cầu cho email này.
2. Mở link reset trong MailHog, đặt mật khẩu mới dài ít nhất 10 ký tự.
3. Thử mật khẩu cũ một lần, sau đó đăng nhập bằng mật khẩu mới.
4. Đăng xuất rồi mở lại trang hồ sơ riêng tư.

**Mong đợi:** mật khẩu cũ bị từ chối, mật khẩu mới dùng được; token reset dùng
một lần; sau đăng xuất không còn hiển thị hồ sơ riêng tư. Không nhập sai liên tục
năm lần nếu không chủ động muốn thử khóa tạm 15 phút.

## 3. Tổ chức, nhân viên và tài khoản nhận tiền

### TC-05 — Nộp hồ sơ Organizer · P0

1. Đăng nhập Organizer bằng `organizer@example.test`, mở **Hồ sơ nhà tổ chức**.
2. Nhập tên `UAT-01 — Tổ chức thử nghiệm`, email liên hệ cùng tài khoản,
   địa chỉ `123 Đường Thử Nghiệm, Hà Nội`, giới thiệu có ghi “dữ liệu kiểm thử”.
3. Tải PNG mẫu ở phần chuẩn bị, chờ **Tài liệu đã tải lên** rồi bấm **Gửi hồ sơ**.

**Mong đợi:** hồ sơ chờ xét duyệt; form khóa khi chờ; không tự tạo tổ chức được duyệt.

### TC-06 — Admin từ chối, Organizer sửa và gửi lại · P1

1. Admin → **Duyệt hồ sơ** → tìm đúng tên → **Xem hồ sơ**.
2. Nhập lý do `Bổ sung địa chỉ tầng/phòng`, chọn **Từ chối hồ sơ**. Ở hộp xác
   nhận thử **Hủy** một lần: trạng thái phải giữ nguyên. Sau đó từ chối thật.
3. Quay lại tab Organizer để trạng thái tự cập nhật, đọc lý do; sửa địa chỉ thành
   `123 Đường Thử Nghiệm, tầng 2, Hà Nội`, bấm **Lưu và gửi lại**.
4. Quay lại tab Admin và **Phê duyệt hồ sơ**, xác nhận quyết định.

**Mong đợi:** lý do tới đúng người nộp, gửi lại quay về chờ duyệt; sau phê duyệt
Organizer thấy tổ chức tại **Tổ chức của tôi**. Ghi mã tổ chức: `________`.

### TC-07 — Thêm Staff và xác minh tài khoản nhận tiền · P0

1. Organizer mở tổ chức → **Thành viên**; thêm `staff@example.test` với quyền
   **Soát vé**. Staff phải đã xác thực email ở TC-02.
2. Mở **Tài khoản nhận tiền**; nhập ngân hàng `Ngân hàng kiểm thử`, chủ tài khoản
   `UAT ORGANIZER`, số thử `12345678901234`; bấm **Gửi tài khoản để xác minh**.
3. Admin → **Quản lý tổ chức** → tổ chức tương ứng → **Tài khoản nhận tiền**;
   nhập ghi chú `Xác minh dữ liệu UAT local`, bấm **Xác minh payout**, xác nhận.
4. Quay lại tab Organizer; tài khoản nhận tiền tự cập nhật trạng thái.

**Mong đợi:** Staff là thành viên nhưng chưa có sự kiện được phân công; tài khoản
nhận tiền hoạt động sau Admin duyệt, chỉ hiện bốn số cuối `1234`; ô nhập số đầy
đủ được xóa. Không có lệnh chuyển tiền ngân hàng.

## 4. Luồng chính: một sự kiện từ tạo đến đối soát

### TC-08 — Tạo sự kiện và hạng vé · P0

Đến bước này mới chọn thời gian. Ví dụ bắt đầu thao tác lúc **14:00**:

| Trường | Giá trị mẫu |
| --- | --- |
| Tên | `UAT-01 — Workshop chính` |
| Danh mục | Workshop |
| Mô tả | Workshop kiểm thử mua vé, nhận QR và check-in; dữ liệu local |
| Địa điểm / địa chỉ / thành phố | Phòng UAT / 123 Đường Thử Nghiệm, tầng 2 / Hà Nội |
| Sức chứa | 20 |
| Bắt đầu / kết thúc | Hôm nay 15:00 / 15:30, múi giờ Asia/Ho_Chi_Minh |
| Điều khoản | Mỗi vé vào một lần; mang QR; đây là dữ liệu thử |
| Hạng trả phí | Vé tiêu chuẩn, 100.000đ, quota 15, tối đa 4/đơn |
| Hạng miễn phí | Vé cộng đồng, 0đ, quota 5, tối đa 4/đơn |
| Mở / đóng bán của cả hai hạng | Hôm nay 13:00 / 14:55 |

Thay giờ tương ứng với lúc bạn làm; **mua xong trước giờ đóng bán**. Khi bắt đầu
sự kiện còn trong vòng hai giờ tới thì đã có thể check-in. Nếu cần thêm thời gian,
hãy chọn lịch rộng hơn ngay từ bản nháp; sự kiện công khai không cho đổi giờ chính.

1. Organizer → tổ chức → **Sự kiện và phân công** → nhập tên → **Tạo bản nháp**.
2. Mở **Quản lý sự kiện**. Nếu gặp cảnh báo chưa lưu sau khi vừa tạo thành công,
   có thể xác nhận rời trang để mở bản nháp đã lưu; đây là điểm UX đã ghi nhận.
3. Điền bước **1 Thông tin**, tải ảnh mẫu, bấm **Lưu bản nháp**.
4. Điền **2 Địa điểm** và lịch như bảng, lưu.
5. **3 Hạng vé** → **Thêm hạng vé**; tạo và lưu từng hạng.
6. **4 Xem lại**: kiểm tra mọi nội dung rồi **Gửi kiểm duyệt**, xác nhận.

**Mong đợi:** tổng quota 15+5 không vượt capacity 20; sau gửi thì nội dung/hạng
vé khóa. Khách chưa thấy sự kiện trên trang công khai. Ghi mã sự kiện: `________`.

### TC-09 — Duyệt, công khai và phân công · P0

1. Admin → **Kiểm duyệt sự kiện**, chọn đúng tên, kiểm tra nội dung và hạng vé.
2. **Phê duyệt sự kiện**, xác nhận. Customer tìm tên: vẫn chưa có sự kiện mở bán.
3. Quay lại tab Organizer, trạng thái sự kiện tự cập nhật → **4 Xem lại** → **Công khai sự kiện**, xác nhận.
4. Về **Sự kiện và phân công**; chọn sự kiện và Staff → **Phân công nhân viên**.
5. Customer tìm đúng tên; Staff mở **Check-in sự kiện**.

**Mong đợi:** sau công khai khách thấy ảnh, lịch, địa điểm, hai hạng vé đang bán.
Staff thấy đúng sự kiện, khoảng giờ check-in và nút **Mở soát vé**.

### TC-10 — Khách mua hai vé trả phí và thanh toán QR · P0

1. Trong profile Customer đang đăng xuất, tìm sự kiện → **Chọn vé**.
2. Chọn **Vé tiêu chuẩn = 2**, Vé cộng đồng = 0; tích điều khoản rồi
   **Đăng nhập để tiếp tục**, dùng `buyer@example.test`.
3. Kiểm tra số lượng vẫn là 2; **tích lại điều khoản** sau đăng nhập nếu bị bỏ.
4. **Tiếp tục thanh toán**: kiểm tra đơn 200.000đ, thời hạn giữ vé 10 phút.
5. Tích **Tôi đã kiểm tra đơn và đồng ý điều khoản**, bấm nút thanh toán.
6. Chọn QR → **Hiển thị mã QR**, chờ kết quả tự cập nhật (mặc định khoảng 4 giây).

**Mong đợi:** đơn **Đã xác nhận**, thanh toán thành công 200.000đ; phát đủ 2/2 vé.
Đây là QR provider local, không dùng app ngân hàng để chuyển tiền. Ghi mã đơn:
`________`. Nếu chưa thấy vé, chờ màn hình tự cập nhật sau khi phát hành; không
bấm tạo đơn mới chỉ vì phát hành chưa xong.

### TC-11 — Vé điện tử, PDF và một đơn miễn phí · P0

1. Ở kết quả đơn trả phí chọn **Xem vé của đơn hàng**: phải có đúng hai vé.
2. Mở từng **Xem vé**, ghi hai mã vé, kiểm tra tên/hạng/thời gian/địa điểm và QR.
3. Tải PDF của một vé, mở kiểm tra chữ tiếng Việt và QR; tải lại trang, ID vé không đổi.
4. Mua riêng **1 Vé cộng đồng** của cùng sự kiện; xác nhận điều khoản và bấm
   **Xác nhận đơn miễn phí**.

**Mong đợi:** đơn miễn phí tổng 0đ, không vào cổng QR/thẻ, phát đúng một vé.
Luồng chính lúc này có **2 đơn confirmed, 3 vé**; chỉ một giao dịch thanh toán
trả phí. Ghi mã vé miễn phí: `________`.

### TC-12 — Quét QR, chặn trùng và tra cứu thủ công · P0

1. Staff → sự kiện → **Mở soát vé**, kiểm tra đúng tên và **Đang nhận khách**.
2. Customer mở QR vé trả phí thứ nhất trên màn hình/thiết bị khác. Staff bấm
   **Bật camera**, cho phép camera và quét. Nếu không có camera, dùng **Tra cứu**
   với mã vé và ghi phần camera là `CHƯA CHẠY`, không đánh dấu camera đã đạt.
3. Quét lại QR thứ nhất: phải hiện **Vé đã được sử dụng**, không tăng lượt.
4. Staff → **Tra cứu**, nhập chính xác mã vé trả phí thứ hai → **Tra cứu vé**.
5. Bấm **Xác nhận check-in**, thử **Hủy**: chưa ghi nhận lượt; sau đó xác nhận thật.
6. Mở **Lịch sử**, **Thống kê**; Customer làm mới vé đã dùng.

**Mong đợi:** 2 lượt check-in duy nhất, 1 vé miễn phí chưa dùng, 0 vé vô hiệu,
tỷ lệ 2/3 ≈ 66,7%. Tra cứu đơn thuần chưa ghi nhận vào cửa. Customer thấy vé đã
dùng, QR được ẩn; PDF tải lại của vé đã dùng không còn QR sử dụng.

### TC-13 — Organizer kiểm tra người tham dự, báo cáo và CSV · P0

1. Organizer → sự kiện → **4 Xem lại** → **Người tham dự** → tải danh sách.
2. Về tổ chức → **Báo cáo doanh thu**; nhập mã sự kiện TC-08 vào bộ lọc.
3. Chọn khoảng ngày chứa thời điểm xác nhận đơn và check-in, múi giờ
   Asia/Ho_Chi_Minh → **Áp dụng bộ lọc**; chờ tải xong.
4. Bấm **Xuất CSV cùng snapshot**, mở tệp và so sánh.

**Mong đợi trước ca bổ sung:** 3 vé (2 trả phí + 1 miễn phí), 2 lượt check-in,
2 đơn, doanh thu 200.000đ, phí nền tảng 10.000đ, gateway local 0đ,
thuần 190.000đ; bán 3/20 = 15%; thanh toán thành công 100% (đơn miễn phí không
tính là một payment attempt). CSV có 2 dòng đơn và tổng khớp dashboard. Nếu
projection chưa cập nhật, quan sát thời điểm đồng bộ và tải lại sau vài giây.

### TC-14 — Chặn đối soát trước khi kết thúc · P0

1. Khi sự kiện còn chưa kết thúc, Organizer → **Đối soát sự kiện**.
2. Chọn đúng sự kiện → **Kiểm tra điều kiện**.

**Mong đợi:** không thể tạo yêu cầu; có lý do sự kiện chưa hoàn tất. Không sửa
trạng thái trực tiếp trong database để làm ca kiểm thử này đạt.

### TC-15 — Đối soát sau khi kết thúc · P0

Điều kiện: qua giờ kết thúc đã đặt ở TC-08, tài khoản nhận tiền đã xác minh,
không có ngoại lệ/hoàn tiền của sự kiện chính. Chờ lifecycle đồng bộ rồi tải lại.

1. Organizer kiểm tra điều kiện lại → **Yêu cầu đối soát toàn bộ sự kiện**;
   kiểm tra số tiền 190.000đ, xác nhận **Tạo yêu cầu đối soát**.
2. Admin → **Đối soát sự kiện** → đúng dòng → **Xem chi tiết**.
3. So sánh 200.000đ − 10.000đ − 0đ = 190.000đ và tài khoản đã che số.
4. Nhập lý do kiểm tra → **Bắt đầu xử lý**, xác nhận.
5. Nhập mã `MOCK-SETTLEMENT-UAT01-001`; nội dung `Đã đối chiếu 2 đơn, 3 vé,
   doanh thu 200000, phí 10000, thuần 190000; mô phỏng local.`
6. Để thời điểm trống để lấy giờ hiện tại. **Ghi nhận chi trả mô phỏng** →
   kiểm tra tên/số tiền trong dialog → **Lưu và kiểm tra bằng chứng**.
7. Tải lại trang, mở lại yêu cầu đối soát; Organizer cũng mở chi tiết đối soát.

**Mong đợi:** **Đã hoàn tất / Đã ghi nhận mô phỏng**, bằng chứng/lịch sử đúng;
không còn nút bắt đầu xử lý/chi trả lần hai. Đây là ghi nhận giả lập, không
chuyển tiền thật. Ghi mã đối soát: `________`.

### TC-16 — Đối chiếu cuối · P0

1. Organizer mở lại báo cáo lọc đúng sự kiện, tải CSV mới.
2. Đối chiếu đơn/vé với TC-13 và số đối soát vừa hoàn tất.

**Mong đợi:** doanh thu 200.000đ, phí 10.000đ, thuần 190.000đ, **đối soát hoàn
tất 190.000đ**; 2 đơn/3 vé không bị tạo thêm bởi payout. Đối soát không biến vé
chưa check-in thành một lượt đã vào cửa. Kết thúc luồng chính tại đây.

## 5. Ca lỗi và ca bổ sung — dùng sự kiện riêng

Tạo các sự kiện mới theo TC-08/09, lịch bán còn hiệu lực và check-in đang mở.
Không chạy trên sự kiện chính đã đối soát. Mỗi lần chạy lại ca nên dùng hậu tố
mới trong tên; các ca dưới đây độc lập về dữ liệu với nhau.

| ID / ưu tiên | Fixture và bước thực hiện | Kết quả mong đợi |
| --- | --- | --- |
| TC-17 · P1 — Nháp thiếu / quota sai | Tạo `UAT-17`, chỉ nhập tên rồi gửi duyệt. Sau đó điền đủ, capacity 2 nhưng tạo hạng quota 3. | Nêu nội dung còn thiếu; không gửi thành công. Quota bị từ chối, dữ liệu nhập được giữ để sửa. Khách không thấy bản nháp. |
| TC-18 · P1 — Từ chối sự kiện | Tạo đủ `UAT-18`, gửi duyệt. Admin từ chối với lý do; Organizer tải lại, sửa theo lý do, gửi lại; Admin duyệt rồi Organizer công khai. | Lý do hiển thị, trạng thái quay về chờ duyệt khi gửi lại. Không công khai chỉ nhờ phê duyệt; pending không chỉnh sửa được. |
| TC-19 · P1 — Thẻ thất bại và thử lại | `UAT-19`, vé 100.000đ. Mua 1 vé, chọn thẻ `4000 0000 0000 0002`; sau khi có kết quả thất bại rõ ràng, thử lại bằng `4242 4242 4242 4242` trước khi hết hạn giữ vé. | Lần đầu báo số dư không đủ, chưa phát vé; lần sau đơn confirmed, đúng 1 vé. Giữ nguyên mã đơn và hạn giữ vé; hai lần thử thanh toán. Không dùng số thẻ thật. |
| TC-20 · P1 — Hủy đơn đang giữ vé | `UAT-20`, quota 1. Buyer giữ 1 vé, chưa khởi tạo thanh toán. Ở checkout chọn **Hủy đơn đang giữ vé**; Hủy dialog trước, rồi thực hiện lại và xác nhận. | Hủy dialog giữ đơn; xác nhận hủy chuyển CANCELLED, trả tồn kho, không có vé được phát. |
| TC-21 · P1 — Hết 10 phút giữ vé | `UAT-21`, quota 1. Giữ vé, ghi deadline; reload để đối chiếu, không bắt đầu thanh toán; chờ hơn 10 phút rồi làm mới đơn. | Deadline không gia hạn vì reload; đơn hết hạn, không thanh toán/xác nhận miễn phí được, tồn kho được trả sau worker xử lý. |
| TC-22 · P0 — Tranh vé cuối | `UAT-22`, capacity/quota 1. Buyer và Outsider ở hai profile chọn 1 vé và gần đồng thời bấm tiếp tục thanh toán. | Chỉ một đơn giữ được chỗ; bên còn lại nhận thông báo hết/không đủ vé và có thể giảm lựa chọn. Không có tổng giữ+bán vượt 1. Hủy đơn thắng nếu không mua tiếp. |
| TC-23 · P1 — Dừng và tiếp tục bán | `UAT-23` đã công khai, không có đơn cần bảo toàn. Organizer bước Hạng vé → **Tạm dừng bán**; Customer tải lại. Sau đó Organizer **Tiếp tục bán**. | Khách thấy trạng thái tương ứng, không tạo đơn mới trong lúc dừng; tiếp tục bán cho phép mua trong cửa sổ thời gian. |
| TC-24 · P0 — Hủy sự kiện | `UAT-24` chỉ có vé miễn phí, mua 1 vé chưa dùng, lưu QR. Organizer bước Xem lại → mở **Hủy sự kiện**, ghi lý do và xác nhận. Customer tải lại; Staff thử QR cũ. | Sự kiện hủy, vé bị vô hiệu, QR cũ không vào được; không còn mua mới. Dùng đơn miễn phí để ca này không phát sinh nghĩa vụ hoàn tiền trả phí. |
| TC-25 · P0 — Quyền và thu hồi Staff | `UAT-25`, phân công Staff, phát 1 vé miễn phí chưa dùng. Staff mở máy quét; Organizer thu hồi phân công. Staff tải lại/tra cứu vé. Thử vào cùng sự kiện bằng Outsider chưa là thành viên. | Quyền cũ bị từ chối, không ghi nhận admission; Outsider không xem dữ liệu quản trị của tổ chức. Có thể phân công lại để tiếp tục ca khác. |
| TC-26 · P0 — Vé người khác | Dùng vé Buyer ở TC-11 hoặc một fixture riêng. Sao chép URL vé; mở bằng Outsider đang đăng nhập. | Không đọc được QR/PDF của Buyer; URL biết trước không cấp quyền sở hữu. |
| TC-27 · P1 — Camera bị từ chối / mất mạng | `UAT-27` đang mở check-in, có 1 vé chưa dùng. Từ chối quyền camera rồi dùng link tra cứu. Trước lần thử khác, ngắt mạng/DevTools Offline và quan sát scanner; khôi phục mạng. | Có hướng dẫn/fallback tra cứu; offline không thông báo check-in thành công hoặc lưu hàng đợi vào cửa. Nếu gửi rồi mất phản hồi, dùng **Kiểm tra lại kết quả**, không coi timeout là thất bại chắc chắn. |
| TC-28 · P1 — Khóa tài khoản | Chỉ dùng Outsider. Admin → **Tài khoản người dùng**, khóa với lý do trong khi Outsider có phiên đăng nhập. Outsider tải trang riêng tư. Sau đó Admin mở khóa. | Phiên cũ bị từ chối; mở khóa không hồi sinh phiên đã thu hồi, phải đăng nhập lại. Không khóa Admin bootstrap duy nhất. |
| TC-29 · P1 — Mobile, bàn phím và lưu nháp | Trên fixture nháp riêng, nhập thay đổi chưa lưu rồi rời trang; Hủy cảnh báo. Kiểm tra Customer/Organizer/Admin/Staff ở rộng 390px, Tab/Shift+Tab, dialog Hủy/Escape, nút tăng/giảm số lượng. | Không mất dữ liệu khi hủy điều hướng; control dùng được bằng bàn phím, focus ở trong dialog và quay về nút mở khi đóng; không tràn ngang toàn trang. Bảng rộng được phép cuộn trong vùng bảng. |

Các ca tài chính UNKNOWN, callback đến muộn, hoàn tiền từng phần, crash/recovery
không có đủ thao tác người dùng thông thường để diễn đầy đủ trong bộ thủ công
này. Chúng thuộc các runner kỹ thuật hiện có và không được tính là đã kiểm thử
qua các bước trên. Camera vật lý chỉ được ghi đạt nếu bạn thực sự quét trên thiết bị.

## 6. Ghi kết quả và báo lỗi

Dùng [bảng kết quả trống](manual-uat-results.md); ghi `ĐẠT`, `LỖI`, `CHƯA CHẠY`
hoặc `BỊ CHẶN`, kèm ID sự kiện/đơn/vé và đường dẫn ảnh. Không ghi mật khẩu/token.

Mẫu báo lỗi:

```text
Mã ca / vai trò:
URL và thời điểm:
ID sự kiện / đơn / vé:
Các bước đã làm:
Kết quả mong đợi:
Kết quả thực tế + ảnh:
Lặp lại được không:
Mức ảnh hưởng: chặn luồng / sai dữ liệu / có cách đi vòng / hiển thị
```

Ưu tiên báo ngay các lỗi sai số tiền, bán vượt quota, phát trùng vé, check-in
trùng hoặc đọc được vé/tổ chức người khác. Các điểm UX đã quan sát trước reset
như cảnh báo nháp đã lưu, tích lại điều khoản sau login, lọc báo cáo bằng UUID
vẫn cần ghi nhận nếu bạn gặp; reset dữ liệu không sửa những hành vi này.

Để chạy lại toàn bộ regression tự động sau này, phải tạo lại fixture từ Phase 1
theo [runbook local](../local-development.md); fixture và ID trước reset không
còn dùng được. Bộ thủ công này không yêu cầu chạy script hoặc sửa database.

## 7. Kiểm tra bổ sung sau sửa portal và UI

- Mở trực tiếp `/org/organizations`, `/admin/categories`, `/staff/check-in/events`,
  tải lại trang và dùng Back/Forward; đường dẫn phải giữ đúng context path.
- Đăng ký/xác thực/gửi lại email/reset mật khẩu từ từng portal; mở email ở tab mới.
  Xác thực xong phải có liên kết đăng nhập cùng portal và giữ đích đến nội bộ.
- Mở đồng thời nhiều portal bằng cùng tài khoản: phiên không bị tự thu hồi khi tải.
  Dùng browser profile riêng nếu kiểm tra nhiều tài khoản đồng thời.
- Mỗi ô mật khẩu đều có nút con mắt: bật/tắt độc lập, giữ giá trị, không gửi form;
  có thể dùng Tab và Enter. Kiểm tra cả xác nhận mật khẩu và đổi mật khẩu trong hồ sơ.
- Admin → Danh mục: bấm Ngừng sử dụng, nhập lý do trong hộp thoại; thử lý do trống,
  khoảng trắng, Hủy, xác nhận và Mở lại. Dòng tương ứng phải cập nhật trạng thái.
- Admin → Duyệt hồ sơ: nhãn/giá trị/tài liệu/lý do/quyết định được chia nhóm rõ.
- Organizer → Chi tiết tổ chức: bảy mục điều hướng đồng nhất; quay về từ báo cáo/
  đối soát vẫn đúng tổ chức. Thử màn hình 1440, 768, 390, 320px và zoom 200%.
