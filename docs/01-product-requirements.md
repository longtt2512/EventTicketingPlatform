# Event Ticketing Platform

**Product Requirements Document (PRD) | v1.1.0 | MVP 1.0**

## 1. Document Information

| Attribute | Value |
| --- | --- |
| Product Name | Event Ticketing Platform |
| Document Version | 1.1.0 |
| Release Version | MVP 1.0 |
| Last Updated | 19/11/2026 |
| Scope Status | Finalized: 13 modules |
| Product Type | Event Ticketing Marketplace |
| Business Model | B2B2C |
| Target Market | Vietnam |
| Currency | VND |
| Language | Vietnamese, English |

Phạm vi 13 module được xác nhận. Các lựa chọn về thị trường, mô hình kinh doanh, responsive web, thời gian giữ vé, phí và phương thức đối soát là đề xuất triển khai của PRD, cần xác nhận riêng khi thiết kế hệ thống.

## 2. Product Vision

Event Ticketing Platform là nền tảng trực tuyến kết nối nhà tổ chức sự kiện với khách hàng, hỗ trợ toàn bộ quy trình tổ chức, quảng bá, phân phối, bán vé và kiểm soát người tham dự sự kiện.

Hệ thống hướng đến việc cung cấp một giải pháp Event Ticketing end-to-end, giúp nhà tổ chức giảm chi phí vận hành và khách hàng có trải nghiệm mua vé thuận tiện, nhanh chóng, an toàn.

## 3. Product Objectives

- Số hóa quy trình quản lý và bán vé sự kiện.
- Cho phép nhà tổ chức chủ động tạo và quản lý sự kiện.
- Cung cấp trải nghiệm tìm kiếm, đặt vé và thanh toán trực tuyến.
- Đảm bảo tính nhất quán của dữ liệu vé trong môi trường có nhiều truy cập đồng thời.
- Ngăn chặn overselling, vé giả và sử dụng vé nhiều lần.
- Minh bạch doanh thu, phí dịch vụ và đối soát.
- Xây dựng nền tảng có khả năng mở rộng.

## 4. Product Ecosystem

Hệ thống bao gồm bốn phân hệ sử dụng chung backend:

| Application | Target User |
| --- | --- |
| Customer Application | Khách hàng |
| Organizer Portal | Nhà tổ chức sự kiện |
| Check-in Application | Nhân viên soát vé |
| Admin Portal | Quản trị viên |

MVP ưu tiên Responsive Web Application. Các ứng dụng native dành cho iOS và Android có thể được phát triển trong các phiên bản tiếp theo.

## 5. User Roles

| Role | Responsibility |
| --- | --- |
| GUEST | Xem và tìm kiếm sự kiện |
| CUSTOMER | Đặt vé, thanh toán và quản lý vé cá nhân |
| ORGANIZER | Tổ chức và quản lý sự kiện |
| CHECK_IN_STAFF | Kiểm tra và xác thực vé |
| ADMIN | Quản trị toàn bộ nền tảng |

Một người dùng có thể sở hữu nhiều vai trò. Quyền của Organizer và Check-in Staff phải được giới hạn theo tổ chức và sự kiện được phân công.

## 6. Functional Requirements

Toàn bộ 13 module dưới đây thuộc phạm vi MVP 1.0.

### 6.1. Authentication & Authorization

Cho phép người dùng quản lý tài khoản và truy cập hệ thống theo quyền được cấp.

| ID | Requirement |
| --- | --- |
| AUTH-01 | Đăng ký tài khoản bằng email |
| AUTH-02 | Đăng nhập |
| AUTH-03 | Đăng xuất |
| AUTH-04 | Xác thực email |
| AUTH-05 | Quên mật khẩu |
| AUTH-06 | Đặt lại mật khẩu |
| AUTH-07 | Thay đổi mật khẩu |
| AUTH-08 | Quản lý thông tin cá nhân |
| AUTH-09 | Access Token và Refresh Token |
| AUTH-10 | Role-Based Access Control |
| AUTH-11 | Kiểm soát trạng thái tài khoản |

### 6.2. Organizer Management

Cho phép cá nhân hoặc tổ chức đăng ký trở thành nhà tổ chức sự kiện.

| ID | Requirement |
| --- | --- |
| ORG-01 | Đăng ký trở thành Organizer |
| ORG-02 | Cập nhật thông tin tổ chức |
| ORG-03 | Cung cấp thông tin xác minh |
| ORG-04 | Admin phê duyệt hoặc từ chối Organizer |
| ORG-05 | Quản lý thông tin tài khoản nhận tiền |
| ORG-06 | Quản lý thành viên tổ chức |
| ORG-07 | Thêm và xóa nhân viên soát vé |
| ORG-08 | Phân công nhân viên theo sự kiện |
| ORG-09 | Quản lý trạng thái hoạt động của Organizer |

Chỉ Organizer đã được xác minh mới được gửi sự kiện để kiểm duyệt và mở bán vé.

### 6.3. Event Management

| ID | Requirement |
| --- | --- |
| EVT-01 | Tạo sự kiện |
| EVT-02 | Cập nhật thông tin sự kiện |
| EVT-03 | Upload banner và hình ảnh |
| EVT-04 | Thiết lập danh mục sự kiện |
| EVT-05 | Thiết lập địa điểm |
| EVT-06 | Thiết lập thời gian bắt đầu và kết thúc |
| EVT-07 | Thiết lập sức chứa tối đa |
| EVT-08 | Thiết lập điều khoản tham dự |
| EVT-09 | Gửi sự kiện để kiểm duyệt |
| EVT-10 | Phê duyệt hoặc từ chối sự kiện |
| EVT-11 | Công khai sự kiện |
| EVT-12 | Hủy sự kiện |
| EVT-13 | Quản lý vòng đời sự kiện |
| EVT-14 | Xem danh sách người tham dự |

Các danh mục sự kiện được hỗ trợ: Music & Concert, Sports, Conference, Workshop, Exhibition, Festival, Entertainment, Others.

Mỗi sự kiện trong MVP có một địa điểm và một khoảng thời gian tổ chức.

### 6.4. Ticket Type & Inventory Management

Mỗi sự kiện có thể phát hành nhiều hạng vé khác nhau, ví dụ VVIP, VIP, Standard và Economy.

| ID | Requirement |
| --- | --- |
| INV-01 | Tạo hạng vé |
| INV-02 | Cập nhật thông tin hạng vé |
| INV-03 | Thiết lập giá vé |
| INV-04 | Thiết lập số lượng vé |
| INV-05 | Thiết lập thời gian mở bán |
| INV-06 | Thiết lập thời gian đóng bán |
| INV-07 | Giới hạn số vé trên mỗi đơn hàng |
| INV-08 | Tạm dừng hoặc tiếp tục bán vé |
| INV-09 | Hỗ trợ vé miễn phí |
| INV-10 | Quản lý số lượng vé khả dụng |
| INV-11 | Quản lý số lượng vé đang giữ chỗ |
| INV-12 | Quản lý số lượng vé đã bán |
| INV-13 | Ngăn chặn overselling |

**Available Quantity = Total Quantity − Reserved Quantity − Sold Quantity.**

Tổng số vé của các hạng vé không được vượt quá sức chứa của sự kiện.

MVP sử dụng General Admission Ticketing, không hỗ trợ chọn ghế cụ thể.

### 6.5. Event Discovery

| ID | Requirement |
| --- | --- |
| DSC-01 | Hiển thị sự kiện nổi bật |
| DSC-02 | Hiển thị sự kiện sắp diễn ra |
| DSC-03 | Hiển thị sự kiện phổ biến |
| DSC-04 | Tìm kiếm sự kiện theo tên |
| DSC-05 | Lọc theo danh mục |
| DSC-06 | Lọc theo địa điểm |
| DSC-07 | Lọc theo thời gian |
| DSC-08 | Lọc theo khoảng giá |
| DSC-09 | Xem chi tiết sự kiện |
| DSC-10 | Xem thông tin Organizer |
| DSC-11 | Xem các hạng vé và tình trạng mở bán |

Khách chưa đăng nhập được phép tìm kiếm và xem thông tin sự kiện nhưng phải đăng nhập khi đặt vé.

### 6.6. Ticket Booking & Order Management

| ID | Requirement |
| --- | --- |
| ORD-01 | Lựa chọn hạng vé |
| ORD-02 | Lựa chọn số lượng vé |
| ORD-03 | Kiểm tra số lượng vé khả dụng |
| ORD-04 | Giữ chỗ tạm thời |
| ORD-05 | Tạo đơn hàng |
| ORD-06 | Lưu thông tin giá tại thời điểm đặt vé |
| ORD-07 | Hiển thị tổng giá trị đơn hàng |
| ORD-08 | Hiển thị thời gian giữ vé còn lại |
| ORD-09 | Hủy đơn hàng chưa thanh toán |
| ORD-10 | Tự động hết hạn đơn hàng |
| ORD-11 | Giải phóng vé đã giữ chỗ |
| ORD-12 | Xác nhận đơn hàng thành công |
| ORD-13 | Xem lịch sử đơn hàng |
| ORD-14 | Xem chi tiết đơn hàng |
| ORD-15 | Xử lý đồng thời nhiều yêu cầu đặt vé |

**Business Rules:**

- Một đơn hàng chỉ chứa vé của một sự kiện.
- Một đơn hàng có thể chứa nhiều hạng vé.
- Thời gian giữ vé mặc định là 10 phút.
- Thao tác kiểm tra và giữ vé phải đảm bảo tính nguyên tử.
- Giá vé được lưu dưới dạng snapshot.
- Đơn hàng đã xác nhận không được khách hàng tự ý hủy.

### 6.7. Online Payment Integration

| ID | Requirement |
| --- | --- |
| PAY-01 | Tích hợp Payment Gateway |
| PAY-02 | Khởi tạo giao dịch thanh toán |
| PAY-03 | Chuyển hướng đến trang thanh toán |
| PAY-04 | Tiếp nhận Payment Webhook |
| PAY-05 | Xác thực chữ ký giao dịch |
| PAY-06 | Đối chiếu giá trị và mã giao dịch |
| PAY-07 | Xử lý thanh toán thành công |
| PAY-08 | Xử lý thanh toán thất bại |
| PAY-09 | Xử lý giao dịch trùng lặp |
| PAY-10 | Tra cứu trạng thái giao dịch |
| PAY-11 | Lưu lịch sử thanh toán |
| PAY-12 | Đối soát với Payment Gateway |
| PAY-13 | Xử lý ngoại lệ thanh toán |

MVP chỉ tích hợp một Payment Gateway. Payment Gateway có thể được lựa chọn từ VNPay, MoMo hoặc một nhà cung cấp phù hợp khác.

Backend chỉ xác nhận thanh toán dựa trên thông tin đã được xác thực từ Payment Gateway, không dựa trên trạng thái do frontend cung cấp.

Nếu nhận được giao dịch thành công sau khi giữ chỗ đã hết hạn, hệ thống không tự động phát hành thêm vé mà ghi nhận ngoại lệ để xử lý hoàn trả khoản tiền.

### 6.8. QR Code E-Ticket

| ID | Requirement |
| --- | --- |
| ETK-01 | Tự động phát hành vé sau khi xác nhận đơn hàng |
| ETK-02 | Tạo mã định danh duy nhất cho từng vé |
| ETK-03 | Tạo QR Code riêng cho từng vé |
| ETK-04 | Hiển thị danh sách vé cá nhân |
| ETK-05 | Hiển thị chi tiết vé |
| ETK-06 | Tải vé dưới định dạng PDF |
| ETK-07 | Gửi vé qua email |
| ETK-08 | Hiển thị trạng thái vé |
| ETK-09 | Vô hiệu hóa vé khi sự kiện bị hủy |
| ETK-10 | Thử lại quá trình phát hành vé khi gặp lỗi |

Mỗi vé điện tử bao gồm: Ticket ID, Event Name, Ticket Type, Customer hoặc Attendee Information, Event Date & Time, Venue, QR Code, Ticket Status.

Số lượng vé được phát hành phải bằng số lượng vé trong đơn hàng đã xác nhận.

### 6.9. Check-in Management

| ID | Requirement |
| --- | --- |
| CHK-01 | Xem danh sách sự kiện được phân công |
| CHK-02 | Quét QR Code bằng camera |
| CHK-03 | Xác thực mã vé |
| CHK-04 | Xác thực vé thuộc đúng sự kiện |
| CHK-05 | Kiểm tra thời gian được phép check-in |
| CHK-06 | Ngăn chặn sử dụng vé nhiều lần |
| CHK-07 | Tra cứu vé thủ công |
| CHK-08 | Check-in thủ công đối với vé hợp lệ |
| CHK-09 | Lưu lịch sử check-in |
| CHK-10 | Thống kê số người đã check-in |

Hệ thống phải xác thực vé trực tuyến để đảm bảo tính nhất quán giữa nhiều thiết bị quét đồng thời.

### 6.10. Commission & Settlement

Nền tảng thu phí dịch vụ đối với các vé được bán thành công.

| ID | Requirement |
| --- | --- |
| STL-01 | Admin cấu hình mức phí dịch vụ |
| STL-02 | Hỗ trợ phí theo phần trăm |
| STL-03 | Hỗ trợ phí cố định trên mỗi vé |
| STL-04 | Tính toán phí dịch vụ |
| STL-05 | Lưu Commission Snapshot |
| STL-06 | Ghi nhận phí Payment Gateway |
| STL-07 | Tính doanh thu thuần của Organizer |
| STL-08 | Tạo yêu cầu đối soát |
| STL-09 | Admin kiểm tra yêu cầu đối soát |
| STL-10 | Admin xác nhận hoàn tất chuyển tiền |
| STL-11 | Lưu lịch sử đối soát |
| STL-12 | Xem chi tiết báo cáo đối soát |

Mô hình mặc định: Organizer chịu phí nền tảng và phí thanh toán.

**Net Revenue = Gross Revenue − Platform Fee − Payment Gateway Fee − Applicable Adjustments.**

MVP thực hiện chuyển tiền cho Organizer thủ công và ghi nhận kết quả vào hệ thống.

Không được đối soát các giao dịch đang có ngoại lệ tài chính chưa xử lý.

### 6.11. Reporting & Dashboard

**Organizer Dashboard:**

- Tổng số sự kiện.
- Tổng số vé đã bán.
- Tổng số vé còn lại.
- Tổng doanh thu bán vé.
- Phí dịch vụ.
- Doanh thu thuần.
- Thống kê theo hạng vé.
- Biểu đồ doanh thu theo thời gian.
- Số lượng khách đã check-in.
- Tỷ lệ bán vé.
- Xuất báo cáo CSV.

**Admin Dashboard:**

- Tổng số người dùng.
- Tổng số Organizer.
- Tổng số sự kiện.
- Tổng số vé đã bán.
- Gross Merchandise Value (GMV).
- Doanh thu nền tảng.
- Thống kê giao dịch thanh toán.
- Thống kê đối soát.
- Danh sách ngoại lệ tài chính chưa xử lý.

### 6.12. Admin Portal

| ID | Requirement |
| --- | --- |
| ADM-01 | Quản lý người dùng |
| ADM-02 | Khóa hoặc mở khóa tài khoản |
| ADM-03 | Quản lý Organizer |
| ADM-04 | Xác minh Organizer |
| ADM-05 | Quản lý danh mục sự kiện |
| ADM-06 | Quản lý sự kiện |
| ADM-07 | Kiểm duyệt sự kiện |
| ADM-08 | Quản lý sự kiện nổi bật |
| ADM-09 | Quản lý đơn hàng |
| ADM-10 | Quản lý giao dịch thanh toán |
| ADM-11 | Quản lý ngoại lệ thanh toán |
| ADM-12 | Cấu hình Commission |
| ADM-13 | Quản lý Settlement |
| ADM-14 | Quản lý Audit Log |
| ADM-15 | Quản lý cấu hình hệ thống |

### 6.13. Email Notification

| Trigger | Recipient |
| --- | --- |
| Account Registration | Customer |
| Email Verification | User |
| Password Reset | User |
| Organizer Approved/Rejected | Organizer |
| Event Approved/Rejected | Organizer |
| Order Confirmation | Customer |
| Payment Successful | Customer |
| Ticket Issued | Customer |
| Event Reminder | Customer |
| Event Cancelled | Customer, Organizer, Staff |
| Settlement Completed | Organizer |

Email được gửi bất đồng bộ. Lỗi gửi email không được làm mất vé hoặc đảo ngược giao dịch đã thành công.

## 7. Business State Management

### 7.1. Event Status

`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `PUBLISHED`, `ONGOING`, `COMPLETED`, `CANCELLED`

### 7.2. Organizer Status

`PENDING_VERIFICATION`, `VERIFIED`, `REJECTED`, `SUSPENDED`

### 7.3. Order Status

`PENDING_PAYMENT`, `CONFIRMED`, `EXPIRED`, `CANCELLED`

### 7.4. Payment Status

`PENDING`, `SUCCESS`, `FAILED`, `EXPIRED`, `REFUNDED`

REFUNDED chỉ phục vụ ghi nhận hoàn trả tiền trong các trường hợp ngoại lệ, không đại diện cho chức năng hoàn vé tự phục vụ.

### 7.5. Ticket Reservation Status

`ACTIVE`, `CONSUMED`, `EXPIRED`, `RELEASED`

### 7.6. Ticket Status

`ISSUED`, `CHECKED_IN`, `VOIDED`

### 7.7. Settlement Status

`PENDING`, `PROCESSING`, `SETTLED`, `FAILED`

## 8. Core Business Rules

| ID | Requirement |
| --- | --- |
| BR-01 | Chỉ Organizer được xác minh mới có thể gửi sự kiện để kiểm duyệt |
| BR-02 | Chỉ sự kiện đã được phê duyệt mới được công khai |
| BR-03 | Chỉ bán vé trong thời gian mở bán |
| BR-04 | Tổng số vé phát hành không vượt sức chứa sự kiện |
| BR-05 | Không được overselling |
| BR-06 | Reservation phải có thời gian hết hạn |
| BR-07 | Tự động giải phóng vé hết hạn giữ chỗ |
| BR-08 | Không phát hành vé cho đơn hàng chưa xác nhận |
| BR-09 | Payment Webhook phải được xử lý idempotent |
| BR-10 | Mỗi vé có mã định danh duy nhất |
| BR-11 | Mỗi vé chỉ được check-in thành công một lần |
| BR-12 | Vé đã vô hiệu hóa không được sử dụng |
| BR-13 | Không xóa vật lý sự kiện đã phát sinh giao dịch |
| BR-14 | Thay đổi giá không ảnh hưởng đơn hàng đã tạo |
| BR-15 | Thay đổi phí không ảnh hưởng snapshot của giao dịch đang có |
| BR-16 | Chỉ đối soát khi sự kiện đã kết thúc và không còn nghĩa vụ tài chính chưa xử lý |
| BR-17 | Các thao tác tài chính phải có Audit Log |
| BR-18 | Organizer không được truy cập dữ liệu của tổ chức khác |
| BR-19 | Vé miễn phí không yêu cầu thanh toán qua Payment Gateway |
| BR-20 | Sự kiện bị hủy phải dừng bán vé và dừng check-in |
| BR-21 | Không được giảm hạn mức vé xuống dưới tổng số vé đã bán và đang giữ chỗ |
| BR-22 | Không được phát hành trùng vé khi hệ thống retry |
| BR-23 | Không được thay đổi trạng thái thanh toán thành công bởi callback thất bại đến muộn |
| BR-24 | Không được đối soát cùng một khoản doanh thu nhiều lần |
| BR-25 | Không lưu thông tin thẻ thanh toán nhạy cảm trên hệ thống |

## 9. Exceptional Scenarios

| Scenario | Expected Behavior |
| --- | --- |
| Nhiều khách hàng cùng đặt vé cuối cùng | Chỉ một yêu cầu giữ chỗ thành công |
| Khách không thanh toán trong thời gian giữ vé | Hết hạn đơn hàng và giải phóng vé |
| Payment Webhook gửi nhiều lần | Không ghi nhận trùng hoặc phát hành trùng vé |
| Khách đóng trình duyệt khi thanh toán | Backend tiếp tục xác nhận giao dịch độc lập |
| Thanh toán thành công sau khi hết hạn | Ghi nhận ngoại lệ và xử lý hoàn trả tiền |
| Phát hành vé gặp lỗi | Retry mà không tạo vé trùng |
| Email gửi thất bại | Retry; khách vẫn truy cập được vé trong My Tickets |
| Hai thiết bị quét cùng một QR Code | Chỉ một check-in thành công |
| QR Code thuộc sự kiện khác | Từ chối check-in |
| Organizer thay đổi giá vé | Không ảnh hưởng giá đã lưu trong đơn hàng |
| Sự kiện bị hủy sau khi đã bán vé | Dừng bán vé, vô hiệu hóa vé, thông báo khách hàng và xử lý nghĩa vụ hoàn tiền theo quy trình vận hành |
| Đối soát gặp lỗi | Ghi nhận lỗi, cho phép kiểm tra và xử lý lại mà không chuyển tiền trùng |

Các ngoại lệ liên quan đến khoản tiền đã thu phải được theo dõi cho đến khi xử lý hoàn tất. Trong MVP, việc hoàn trả tiền được thực hiện thủ công thông qua quy trình vận hành hoặc công cụ của Payment Gateway.

## 10. UI/UX Screen Requirements

### 10.1. Customer Application

1. Home Page
2. Event Listing
3. Search Results
4. Event Details
5. Login
6. Register
7. Forgot Password
8. Ticket Selection
9. Checkout
10. Payment Result
11. My Orders
12. Order Details
13. My Tickets
14. Ticket Details
15. User Profile

### 10.2. Organizer Portal

1. Organizer Registration
2. Organizer Dashboard
3. My Events
4. Create Event
5. Edit Event
6. Event Details
7. Ticket Type Management
8. Order Management
9. Attendee Management
10. Check-in Dashboard
11. Staff Management
12. Revenue & Analytics
13. Settlement Management
14. Organization Settings

### 10.3. Check-in Application

1. Assigned Events
2. QR Code Scanner
3. Ticket Validation Result
4. Manual Ticket Lookup
5. Check-in History
6. Check-in Statistics

### 10.4. Admin Portal

1. Admin Dashboard
2. User Management
3. Organizer Management
4. Organizer Verification
5. Event Management
6. Event Approval
7. Category Management
8. Order Management
9. Payment Management
10. Payment Exception Management
11. Commission Configuration
12. Settlement Management
13. Audit Logs
14. System Settings

## 11. Conceptual Domain Model

| Domain | Main Entities |
| --- | --- |
| Identity | User, Role, UserRole, RefreshToken |
| Organization | Organization, OrganizationMember |
| Event | Event, Category, Venue, EventStaff |
| Inventory | TicketType, TicketInventory |
| Reservation | TicketReservation, ReservationItem |
| Order | Order, OrderItem |
| Payment | Payment, PaymentTransaction, PaymentException |
| Ticket | Ticket |
| Check-in | CheckIn |
| Finance | CommissionConfig, CommissionSnapshot, Settlement, SettlementItem |
| Notification | Notification, EmailLog |
| Administration | AuditLog, SystemConfig |

Đây là mô hình dữ liệu nghiệp vụ ở mức khái niệm, chưa phải thiết kế cơ sở dữ liệu vật lý.

## 12. Non-functional Requirements

| Category | Target |
| --- | --- |
| Availability | 99.9% |
| Concurrent Users | 5,000 |
| Read API Latency | P95 ≤ 500 ms |
| Booking API Latency | P95 ≤ 1,000 ms |
| Check-in API Latency | P95 ≤ 1,000 ms |
| Ticket Issuance | P95 ≤ 30 seconds |
| Data Consistency | Strong consistency for Inventory and Check-in |
| Security | HTTPS, RBAC, JWT |
| Payment Security | Signature verification, Idempotency |
| Observability | Logging, Metrics, Distributed Tracing |
| Auditability | Business and Financial Audit Logs |
| Scalability | Horizontal Scaling |
| Disaster Recovery | RPO ≤ 5 minutes, RTO ≤ 60 minutes |
| Accessibility | WCAG 2.2 AA target |
| Responsive Design | Desktop, Tablet, Mobile |

Các chỉ tiêu hiệu năng là mục tiêu kỹ thuật đề xuất, cần được kiểm chứng trong quá trình kiểm thử.

## 13. Success Metrics

| Metric | Target |
| --- | --- |
| Overselling Incidents | 0 |
| Duplicate Ticket Issuance | 0 |
| Duplicate Successful Check-in | 0 |
| Duplicate Settlement | 0 |
| Ticket Issuance Success Rate | ≥ 99.9% |
| Payment Success Rate | ≥ 95% |
| Platform Availability | ≥ 99.9% |
| Average Online Check-in Time | ≤ 3 seconds |

## 14. Out of Scope

Các tính năng không thuộc phạm vi MVP 1.0:

- Refund Management độc lập.
- Customer Self-service Refund.
- Automated Refund.
- Interactive Seat Map.
- Reserved Seating.
- Virtual Waiting Queue.
- Advanced Anti-bot System.
- Promotion & Coupon.
- Early Bird Pricing Automation.
- Group Ticket.
- Multiple Event Sessions.
- Ticket Transfer.
- Ticket Resale Marketplace.
- Dynamic QR Code.
- Offline Check-in.
- Waitlist.
- AI Event Recommendation.
- Social Login.
- Push Notification.
- SMS Notification.
- Automated Organizer Payout.
- Apple Wallet / Google Wallet.
- Native iOS / Android Application.

Các yêu cầu bảo mật cơ bản, chống lạm dụng API và xử lý ngoại lệ tài chính vẫn thuộc phạm vi MVP.

## 15. MVP Acceptance Criteria

MVP được coi là hoàn thành khi đáp ứng toàn bộ các tiêu chí sau:

- Người dùng có thể đăng ký, đăng nhập và quản lý tài khoản.
- Organizer có thể đăng ký và được Admin xác minh.
- Organizer có thể tạo sự kiện với nhiều hạng vé.
- Admin có thể kiểm duyệt sự kiện.
- Organizer có thể công khai sự kiện.
- Customer có thể tìm kiếm, đặt vé và thanh toán.
- Hệ thống tự động giữ chỗ và giải phóng vé hết hạn.
- Hệ thống đảm bảo không xảy ra overselling.
- Hệ thống xác thực và chống xử lý thanh toán trùng lặp.
- Hệ thống phát hành vé điện tử kèm QR Code.
- Customer có thể xem và tải vé.
- Check-in Staff có thể xác thực vé bằng QR Code.
- Hệ thống ngăn chặn check-in nhiều lần.
- Organizer có thể theo dõi doanh thu và số lượng vé.
- Admin có thể cấu hình phí và thực hiện đối soát.
- Hệ thống gửi email theo các nghiệp vụ đã định nghĩa.
- Các ngoại lệ thanh toán được ghi nhận và có quy trình xử lý.
- Toàn bộ nghiệp vụ quan trọng được kiểm thử end-to-end.

## 16. Product Scope Management

Toàn bộ 13 module trong PRD v1.1.0 thuộc phạm vi MVP 1.0.

Các yêu cầu bổ sung ngoài phạm vi phải được đánh giá thông qua Change Request trước khi đưa vào quá trình phát triển.

Các quyết định triển khai cần xác nhận trong giai đoạn System Design bao gồm Payment Gateway, chính sách xử lý ngoại lệ thanh toán, mức phí dịch vụ, công nghệ triển khai, mô hình hạ tầng và chỉ tiêu kiểm thử tải.

## Appendix A. End-to-End Business Workflow

1. **Organizer Onboarding:** Đăng ký nhà tổ chức → Admin xác minh.
2. **Event Creation:** Tạo sự kiện → cấu hình hạng vé → kiểm duyệt → công khai.
3. **Event Discovery:** Khách tìm kiếm sự kiện và lựa chọn hạng vé.
4. **Ticket Booking:** Kiểm tra tồn kho → giữ vé 10 phút → tạo đơn hàng.
5. **Payment Processing:** Thanh toán → xác thực giao dịch → xác nhận đơn hàng. Đơn hàng miễn phí bỏ qua Payment Gateway.
6. **Ticket Issuance:** Phát hành vé → tạo QR Code → gửi email.
7. **Event Check-in:** Quét QR Code → xác thực → ghi nhận người tham dự.
8. **Commission & Settlement:** Kết thúc sự kiện → tổng hợp doanh thu → tính phí → đối soát.

## Appendix B. Proposed Development Phases

| Phase | Scope |
| --- | --- |
| 1. Foundation | Authentication & Authorization; Organizer Management |
| 2. Event Platform | Event Management; Ticket Type & Inventory Management; Event Discovery |
| 3. Transaction Processing | Ticket Booking & Order Management; Online Payment Integration |
| 4. Ticket Fulfillment | QR Code E-Ticket; Check-in Management |
| 5. Platform Operations | Commission & Settlement; Reporting & Dashboard; Admin Portal; Email Notification |
| 6. Quality Assurance | Integration Testing; End-to-End Testing; Performance Testing; Security Testing |

Các chức năng Admin và Email cần thiết cho từng nghiệp vụ được triển khai song song từ các phase trước, thay vì chờ Phase 5.

## Appendix C. Proposed Project Documentation

- `docs/01-product-requirements.md`
- `docs/02-business-rules.md`
- `docs/03-ui-ux-design.md`
- `docs/04-system-design.md`
- `docs/05-database-design.md`
- `docs/06-api-specification.md`
- `docs/07-security-design.md`
- `docs/08-test-strategy.md`
- `docs/09-product-backlog.md`
- `docs/10-deployment-guide.md`

Các tài liệu tiếp theo cần bám sát phạm vi 13 module và business rules trong PRD. PRD không thay thế System Design hay API Specification. Không tự động mở rộng phạm vi MVP với các tính năng trong mục Out of Scope.
