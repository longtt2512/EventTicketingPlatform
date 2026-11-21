# Event Ticketing Platform

**UI/UX Design Document | v1.0.0 | MVP 1.0**

Phiên bản 19/11/2026 · Đặc tả thiết kế giao diện và tương tác

## 1. Mục đích và phạm vi

Tài liệu này cụ thể hóa `01-product-requirements.md` (PRD v1.1.0) thành hướng dẫn thiết kế và triển khai frontend. PRD là nguồn sự thật về nghiệp vụ; tài liệu này là nguồn sự thật về trải nghiệm, bố cục, navigation và trạng thái giao diện. Nếu có mâu thuẫn, ưu tiên PRD và ghi nhận quyết định cần xác nhận; không tự thêm tính năng ngoài phạm vi.

| Thuộc tính | Quyết định thiết kế |
|---|---|
| Sản phẩm | Event Ticketing Platform (ETP) |
| Phạm vi | 13 module MVP, 49 màn hình nghiệp vụ + 2 màn hình xác thực bổ trợ |
| Phân hệ | Customer, Organizer, Check-in, Admin |
| Nền tảng | Responsive Web; check-in ưu tiên trình duyệt điện thoại |
| Ngôn ngữ | VI mặc định, EN tùy chọn; tất cả chuỗi UI dùng i18n |
| Tiền tệ / thời gian | VND; hiển thị múi giờ sự kiện, mặc định Asia/Ho_Chi_Minh |
| Thiết kế | Light theme; màu thương hiệu đề xuất Indigo; không yêu cầu dark mode |
| Tham chiếu nghiệp vụ | PRD v1.1.0, mục 6–15 |

**Không thuộc MVP:** chọn ghế, hàng đợi ảo, mã ưu đãi, nhiều suất diễn, resale/transfer, offline check-in, dynamic QR, native mobile, social login, hoàn tiền tự phục vụ hoặc tự động. Ngoại lệ tài chính vẫn phải được quản trị và ghi nhận hoàn trả thủ công.

### 1.1. Design principles

- Customer: tìm được sự kiện, nhìn thấy giá và tình trạng bán, mua vé với ít bước nhất.
- Organizer: tập trung vào checklist công bố, quản lý tồn kho và doanh thu; luôn thấy phạm vi tổ chức/sự kiện đang thao tác.
- Check-in: mobile-first, thao tác một tay, kết quả rõ trong vài giây, không dựa vào màu sắc duy nhất.
- Admin: ưu tiên dữ liệu, kiểm duyệt và audit trail; mọi thao tác tài chính/rủi ro có bước xác nhận.
- Trạng thái thật từ backend là nguồn dữ liệu quyết định; không giả định thành công từ frontend.

### 1.2. Personas và mục tiêu

| Persona | Mục tiêu chính | Thiết bị ưu tiên | Chỉ dấu UX |
|---|---|---|---|
| Guest | Khám phá sự kiện phù hợp | Mobile/Desktop | Tìm và xem chi tiết không đăng nhập |
| Customer | Đặt vé, trả tiền, lấy QR | Mobile | Giá minh bạch; không mất thông tin khi chuyển hướng |
| Organizer | Công bố, bán vé, theo dõi tài chính | Desktop | Biết chính xác bước nào chưa hoàn thành |
| Check-in Staff | Quét và xác minh vé | Mobile | Kết quả trực quan; không check-in trùng |
| Admin | Duyệt và giải quyết ngoại lệ | Desktop | Dữ liệu có thể truy vết, thao tác an toàn |

## 2. Information architecture & sitemap

Routes bên dưới là đề xuất hợp đồng frontend, không phải API endpoints. `:id` là ID tương ứng; `:orgId` xác định organization context. Backend kiểm tra quyền trên mọi tài nguyên; URL hoặc role ở client không tạo quyền truy cập. Cấu trúc phân hệ có thể nằm chung một web app với route groups.

### 2.1. Customer sitemap

```text
/                         Home
├─ /events                Event listing
│  └─ /events/search      Search results (query parameters)
│     /events/:eventId    Event details
│        └─ /booking/:eventId  Ticket selection
├─ /auth                  Login / Register / Forgot / Reset / Verify
├─ /checkout/:orderId     Checkout (active reservation)
├─ /payment/result/:orderId   Payment result
├─ /account/orders        My orders
│  └─ /account/orders/:orderId  Order details
├─ /account/tickets       My tickets
│  └─ /account/tickets/:ticketId Ticket details
└─ /account/profile       Profile / Change password
```

Customer header: logo, Khám phá, search, language, avatar/account (hoặc Đăng nhập). Desktop account menu chứa Đơn hàng, Vé của tôi, Hồ sơ, workspace có quyền, Đăng xuất. Mobile có bottom navigation Trang chủ / Khám phá / Vé của tôi / Tài khoản; ẩn trong checkout để tập trung vào thanh toán. Dùng breadcrumbs ở desktop, back button ở mobile. Footer có liên hệ hỗ trợ, điều khoản và quyền riêng tư (nội dung pháp lý cần cung cấp).

### 2.2. Organizer sitemap

```text
/organizer/register       Onboarding / verification status
/organizer/:orgId
├─ /dashboard             Overview
├─ /events                My events
│  ├─ /new                Create event (wizard)
│  └─ /:eventId           Event overview
│     ├─ /edit            Edit event
│     ├─ /tickets         Ticket types & inventory
│     ├─ /orders          Event orders
│     ├─ /attendees       Attendees
│     ├─ /check-in        Check-in dashboard
│     └─ /staff           Event staff assignment
├─ /staff                 Organization staff/members
├─ /analytics             Revenue & analytics
├─ /settlements           Settlement list & detail
└─ /settings              Organization & payout settings
```

Sidebar: Tổng quan, Sự kiện, Nhân sự, Báo cáo, Đối soát, Cài đặt. Event detail có secondary tabs Tổng quan / Hạng vé / Đơn hàng / Người tham dự / Check-in / Nhân sự; chỉnh sửa là action riêng. Header có organization selector (nếu nhiều tổ chức), workspace switcher và avatar. Khi đổi tổ chức phải reset filter/dữ liệu cũ; xác nhận nếu có form chưa lưu. Organizer chưa được xác minh nhìn thấy onboarding/status thay vì các chức năng bị khóa.

### 2.3. Check-in sitemap

```text
/check-in/events                   Assigned events
└─ /check-in/events/:eventId
   ├─ /scan                        QR scanner
   ├─ /result                      Latest result (view state)
   ├─ /lookup                      Manual ticket lookup
   ├─ /history                     Check-in history
   └─ /stats                       Check-in statistics
```

Mobile top bar: sự kiện hiện tại, trạng thái kết nối, đổi sự kiện/back. Bottom navigation trong event context: Quét vé / Tra cứu / Lịch sử / Thống kê. Kết quả hiển thị full-screen overlay hoặc dedicated view; không chứa QR/token/PII trên URL. Chỉ assigned event được truy cập. Thay đổi event đóng camera và xóa kết quả cũ.

### 2.4. Admin sitemap

```text
/admin/dashboard
/admin/users                     User management
/admin/organizers                Organizer management
/admin/organizers/verification   Verification queue
/admin/events                    Event management (featured)
/admin/events/approval           Approval queue
/admin/categories                Category management
/admin/orders                    Order management
/admin/payments                  Payment management
/admin/payment-exceptions        Financial exceptions
/admin/commissions               Commission configuration
/admin/settlements               Settlement management
/admin/audit-logs                 Audit logs
/admin/settings                   System settings
```

Sidebar groups: Tổng quan; Người dùng & Organizer; Sự kiện (Danh sách / Kiểm duyệt / Danh mục); Giao dịch (Đơn hàng / Thanh toán / Ngoại lệ); Tài chính (Phí / Đối soát); Hệ thống (Audit / Cài đặt). Detail routes `/:id` hoặc drawer với URL state; moderation detail có ID và link copy được. Admin không được gán quyền tự phục vụ từ UI. Workspace switcher chỉ hiện workspace người dùng thực sự có quyền.

### 2.5. Navigation và route guards

| Trường hợp | Hành vi |
|---|---|
| Guest truy cập route riêng tư | Redirect login, lưu internal return path an toàn; quay lại sau xác thực |
| Email chưa xác minh | Nhắc xác minh, cho gửi lại có cooldown; chặn thao tác yêu cầu verified theo PRD/design đề xuất |
| Session hết hạn | Thử refresh; nếu thất bại chuyển login, không gửi lại mutation tiền tự động; khi trở lại tải trạng thái đơn |
| Không có role hoặc membership | 403; quay lại workspace được phép; không hiển thị dữ liệu trước khi guard xác nhận |
| Resource không tồn tại / ngoài phạm vi | 404/403 tùy backend; không tiết lộ dữ liệu tổ chức khác |
| Tài khoản bị khóa | Thông báo trạng thái, không truy cập chức năng riêng tư; liên hệ hỗ trợ |
| Thay đổi trang khi form dirty | Hỏi Rời trang / Tiếp tục chỉnh sửa; không mất draft đã lưu server |
| Deep link, back, reload | Giữ filter qua query; load lại từ server với order/ticket/settlement |

## 3. User flows

Mỗi flow có điểm vào, nhánh lỗi, điểm kết thúc và điều kiện kiểm thử. Ký hiệu `→` là hành động/điều hướng, không phải sự chuyển trạng thái backend mặc định.

### 3.1. Customer: khám phá và mua vé (UF-C01)

```text
Home / Search → Event Details → Select ticket types + quantity
                                 │
                       Guest? → Login / Verify → Return selection
                                 ↓
                       Server reserves + creates order
                      ┌──────────┴──────────┐
                 No inventory           Reserved
                 Refresh choices           ↓
                                     Checkout + timer
                      ┌──────────┬──────────┴──────────┐
                   Cancel       Expired            Confirm order
                release hold   release hold             ↓
                                  Total = 0? ─ yes → Confirm free
                                      no ↓                 │
                                 Payment gateway           │
                                      ↓                    │
                              Backend confirms status ←────┘
                              ┌───────┴────────┐
                         Pending/fail       CONFIRMED
                         check/retry*             ↓
                                          Issuing → My tickets
```

* Chỉ retry nếu backend xác nhận có thể tạo lần thanh toán mới, reservation còn hiệu lực và không có giao dịch đang chờ. Payment return không phải bằng chứng đã thanh toán. Nếu đã trả tiền sau khi hết hạn, hiển thị ngoại lệ đang xử lý và mã tham chiếu; không cấp vé. Free order không điều hướng gateway. Đăng nhập không tạo reservation trước khi user xác nhận.

### 3.2. Customer: quản lý đơn và vé (UF-C02)

My Orders → Order Details → xem trạng thái đơn, thanh toán, vé → My Tickets → Ticket Details → hiển thị QR / tải PDF. Nếu phát hành chưa xong, hiển thị đang phát hành và cập nhật lại trạng thái, không tạo thêm đơn. Vé VOIDED không hiển thị QR có thể dùng. CHECKED_IN hiển thị thời điểm đã sử dụng; không cho check-in lại. Không có CTA hoàn vé tự phục vụ.

### 3.3. Organizer: onboarding (UF-O01)

Customer → Organizer Registration → nhập hồ sơ, thông tin nhận tiền (tùy yêu cầu xác minh) → PENDING_VERIFICATION → Admin review → VERIFIED hoặc REJECTED. Khi bị từ chối hiển thị lý do, sửa hồ sơ và gửi lại. SUSPENDED không cho mở bán/chỉnh sửa nghiệp vụ bị giới hạn; nội dung read-only theo quyền. Không mở quyền tổ chức trước khi backend xác minh.

### 3.4. Organizer: tạo và công bố sự kiện (UF-O02)

```text
My Events → Create Event wizard
  [1 Basic info] → [2 Venue & time] → [3 Tickets] → [4 Review]
      ↓                  ↓                 ↓             ↓
    Save draft         Validate          Capacity     Submit review
                                                         ↓
                                                   PENDING_REVIEW
                                           ┌─────────────┴─────────┐
                                        REJECTED                APPROVED
                                     Edit + resubmit               ↓
                                                        Organizer publishes
                                                                  ↓
                                                         PUBLISHED → ONGOING
                                                                  ↓
                                                              COMPLETED
```

Wizard được phép lưu draft thiếu dữ liệu không bắt buộc tại thời điểm lưu. Submit cần trường bắt buộc, banner, ngày hợp lệ, ít nhất một hạng vé hợp lệ, organizer VERIFIED và tổng capacity hợp lệ. Nếu sự kiện bị hủy, xác nhận tác động và thực hiện quy trình tài chính; không xóa vật lý. Chính sách chỉnh sửa sau phê duyệt cần chốt; đề xuất chặn sửa trường trọng yếu đã bán vé.

### 3.5. Organizer: quản lý vận hành (UF-O03)

Event overview → Ticket types (quan sát total/reserved/sold/available, pause/resume) → Orders / Attendees → Staff (gán người được phép) → Check-in dashboard. Sau khi event COMPLETED → Analytics → Settlements → kiểm tra khoản đủ điều kiện → tạo yêu cầu → theo dõi PENDING / PROCESSING / SETTLED / FAILED. Không tạo yêu cầu trùng hoặc bỏ qua ngoại lệ.

### 3.6. Check-in: xác thực tại cổng (UF-S01)

```text
Assigned events → Choose event → Scanner
                                  ↓
                   Camera permitted? ─ no → Manual lookup
                                  yes              ↓
                               Scan QR        Find ticket
                                  ↓                ↓
                           Validate + check-in ← confirm
                                  ↓
                 ┌───────────────┼─────────────────┐
             SUCCESS          REJECTED          UNKNOWN
          Show admission      Show reason       Network error
                 ↓                ↓              Check status
             Scan next        Scan next        before next scan
```

Quét QR hợp lệ thực hiện check-in nguyên tử trên server. Tra cứu thủ công chỉ xem, cần CTA xác nhận check-in. Nếu mất mạng, không ghi nhận thành công tại client, không có offline queue. Nếu request timeout, kiểm tra kết quả trước khi thử lại với định danh request phù hợp; tránh xác nhận cổng sai.

### 3.7. Admin: kiểm duyệt (UF-A01)

Dashboard/Queue → Organizer verification / Event approval → mở hồ sơ và kiểm tra → Approve hoặc Reject (bắt buộc lý do) → server xác nhận → refresh queue / detail → email thông báo. Nếu người khác đã xử lý, reload dữ liệu và không ghi đè. Duyệt sự kiện không tự động publish.

### 3.8. Admin: ngoại lệ tài chính và đối soát (UF-A02)

Payment exceptions → kiểm tra order/payment và hồ sơ từ gateway → xác định khoản cần hoàn trả → thực hiện ngoài hệ thống theo quy trình được duyệt → nhập mã tham chiếu/bằng chứng và xác nhận → audit. Không có nút tự động refund. Settlement queue → kiểm tra event COMPLETED, snapshot, ngoại lệ, tài khoản nhận tiền → ghi nhận PROCESSING → chuyển tiền thủ công → nhập reference và xác nhận SETTLED; nếu thất bại ghi lý do. Không đánh dấu SETTLED nếu chưa có bằng chứng, không chuyển tiền lại khi trạng thái chưa rõ.

## 4. Visual design system

Đây là định hướng giao diện đề xuất cho MVP, không phải bộ nhận diện thương hiệu đã được duyệt. Customer dùng bề mặt sáng, ảnh sự kiện lớn, CTA nổi bật. Organizer/Admin ưu tiên mật độ thông tin vừa phải, nền trung tính. Check-in sử dụng nền tương phản và thao tác lớn.

| Token | Value | Usage |
|---|---|---|
| primary / hover | #4338CA / #3730A3 | CTA chính, link, active nav |
| primary-soft | #EEF2FF | Selection, information surface |
| text / secondary | #111827 / #475569 | Nội dung, nhãn phụ |
| background / surface | #F8FAFC / #FFFFFF | Nền / card |
| border | #CBD5E1 | Border, divider; control cần tương phản phù hợp |
| success | #166534; bg #DCFCE7 | Xác nhận, check-in thành công |
| warning | #92400E; bg #FEF3C7 | Chờ, sắp hết hạn |
| danger | #B91C1C; bg #FEE2E2 | Lỗi, hủy, từ chối |
| info | #1E40AF; bg #DBEAFE | Thông tin, processing |

Typography: Be Vietnam Pro (đề xuất), fallback system sans-serif hỗ trợ tiếng Việt. H1 32/40 desktop, 26/34 mobile; H2 24/32; H3 18/26; body 16/24; compact table 14/20; caption 12/18. Số tiền, timer và KPI dùng tabular numerals. Không dùng chữ nhỏ cho nội dung quan trọng.

Spacing: 4, 8, 12, 16, 24, 32, 48, 64 px; radius 8 cho input/button, 12 cho card, 16 cho hero; shadow nhẹ; icon đơn giản 20–24 px. Button height 44 px tối thiểu với mobile, check-in 56 px CTA chính. Focus ring 2–3 px có độ tương phản rõ. Không chỉ dùng màu để truyền đạt trạng thái.

### 4.1. Responsive & layout grid

| Viewport | Grid & layout |
|---|---|
| 360–767 px | 4 cột, gutter 16, padding 16; event cards 1–2 cột tùy width; checkout 1 cột; mobile nav |
| 768–1199 px | 8 cột, gutter 20, padding 24; cards 2–3 cột; portal sidebar thu gọn/drawer |
| ≥1200 px | 12 cột, gutter 24, content max 1280 cho customer; portal sidebar 248, top bar 64, content linh hoạt |

Hỗ trợ 320 px và 200% zoom không mất chức năng. Portal tables có horizontal scroll bên trong vùng bảng hoặc card mobile; giữ thao tác và ID/status quan trọng. Dialog ở mobile chuyển thành full-screen/bottom sheet khi cần, không che CTA hoặc field focus. Check-in dùng safe area và đảm bảo portrait layout, camera tự thích ứng màn hình ngang.

### 4.2. Component catalogue

| Component | Behavior / variants |
|---|---|
| Button | Primary / secondary / text / danger; disabled, loading; spinner không đổi chiều rộng; chống nhấn lặp |
| Input / select / date | Label luôn hiện, required, hint, inline error, keyboard; server validation là quyết định cuối |
| Search & filter | Debounce gợi ý 300 ms nếu tìm live; Enter submit; chip filter, reset, count; URL query đồng bộ |
| Event card | Ảnh 16:9, tên, ngày, địa điểm, giá từ, trạng thái bán; cả card truy cập detail; alt text phù hợp |
| Ticket type card | Hạng vé, mô tả, giá, trạng thái, min/max/remaining hợp lệ, stepper; disabled có lý do |
| Reservation timer | Đồng bộ expiresAt từ server; countdown client chỉ minh họa; reload và tab focus resync; cảnh báo còn ít thời gian |
| Order summary | Hạng vé × số lượng, subtotal, phí khách trả nếu có, total; sticky desktop; không giấu phí |
| Status badge | Nhãn chữ + icon tùy trạng thái; mapping mục 6, không thay trạng thái server |
| Data table | Search, filter, sort, pagination server-side; skeleton, no data, no results, partial/error; giữ cột chính |
| KPI card / chart | Giá trị, đơn vị, period, data updated; tooltip và bảng dữ liệu tương đương; empty ≠ 0 |
| Modal / drawer | Xác nhận hành động, lý do (nếu bắt buộc), focus trap, Escape phù hợp, trả focus về trigger |
| Toast / inline alert | Toast cho mutation thành công thông thường; lỗi tiền/scan/expiry dùng inline persistent; không chỉ dựa vào toast |
| QR ticket / scanner | QR đủ lớn, quiet zone, không che; scan overlay, camera permission, fallback lookup, trạng thái kết nối |
| Stepper / wizard | Bước hiện tại, completed, lỗi; back giữ dữ liệu; save draft rõ ràng; submit review cuối |
| Upload | Preview, progress, replace/remove, loại file và giới hạn từ backend; lỗi không làm mất form |

## 5. Layout patterns & low-fidelity wireframes

Các sơ đồ là wireframe cấu trúc (không phải mockup pixel-perfect). Các nhãn tiếng Anh trong sơ đồ chỉ biểu thị vùng chức năng; UI thực tế dùng i18n.

### 5.1. Customer home & event detail

```text
DESKTOP HOME                         DESKTOP EVENT DETAIL
+--------------------------------+   +--------------------------------+
| Logo   Discover  Search   User |   | Logo   Discover  Search   User |
| [ Hero banner / CTA         ]  |   | Breadcrumb                     |
| Categories [ ] [ ] [ ] [ ]     |   | [ Event banner              ]  |
| Featured events                |   | Event title       +-----------+|
| [Card] [Card] [Card]           |   | Date / Venue      | From VND  ||
| Upcoming events                |   | Organizer          | Sale info ||
| [Card] [Card] [Card]           |   | Description        | Buy ticket||
| Footer                         |   | Ticket types       +-----------+|
+--------------------------------+   | Terms / support                 |
                                     +--------------------------------+
```

Mobile home: search ở đầu, hero 16:9, chip danh mục cuộn ngang, card, bottom nav. Mobile detail: banner, title/meta, organizer, nội dung, ticket preview; sticky bottom CTA kèm giá từ, trạng thái đóng bán hoặc sold out. Sticky CTA không che nội dung cuối.

### 5.2. Ticket selection / checkout / payment result

```text
+----------------------------------------------------+
| Logo                         Secure checkout       |
| Select tickets > Checkout > Result                 |
| [Timer: 09:42 remaining] (checkout only)           |
| +-------------------------+ +--------------------+ |
| | Event details           | | Order summary      | |
| | VIP     1.500.000       | | VIP x 2            | |
| |                 [-]2[+] | | Standard x 1       | |
| | Standard  800.000       | | Total              | |
| |                 [-]1[+] | | [Continue / Pay]    | |
| | Contact & terms*        | | Cancel pending*    | |
| +-------------------------+ +--------------------+ |
+----------------------------------------------------+
```

* Checkout only; selection chưa giữ chỗ. Mobile hiển thị list vé rồi summary, CTA sticky khi phù hợp và timer cố định trước summary. Result dùng trung tâm trang + mã đơn, trạng thái payment riêng ticket issuance, CTA tiếp theo rõ ràng.

### 5.3. Organizer portal & wizard

```text
+------------------------------------------------------+
| Logo | Organization selector           User         |
|------+-----------------------------------------------|
| Side | Breadcrumb / Event title [Status] [Action]    |
| nav  | Overview | Tickets | Orders | Staff ...       |
|      | +--------+ +--------+ +--------+              |
|      | | Sold   | |Revenue | |Check-in|              |
|      | +--------+ +--------+ +--------+              |
|      | [Filters]                        [Export]     |
|      | Table / Chart / Event checklist                |
+------------------------------------------------------+
Wizard: [1 Basics] [2 Venue & time] [3 Tickets] [4 Review]
        [ Main form / inline validation ] [Preview]
        [Back] [Save draft]                   [Next]
```

Dashboard không trộn KPI của tổ chức khác. Mobile sidebar thành drawer, tabs cuộn ngang; wizard form một cột, preview đặt sau form, action bar cuối màn hình.

### 5.4. Check-in scanner & result

```text
+------------------------+  +------------------------+
| Back  Event  Online   |  | Event     Result       |
|                        |  |                        |
| +--------------------+ |  |      [CHECK ICON]      |
| | Camera preview     | |  |   CHECK-IN SUCCESS     |
| |     +--------+     | |  | Name / Ticket type     |
| |     |  QR    |     | |  | Ticket ID / Time       |
| |     +--------+     | |  |                        |
| +--------------------+ |  | [Scan next ticket]     |
| Point camera at QR    |  | [Back to event]        |
| [Manual lookup]        |  |                        |
| Scan Lookup History   |  |                        |
+------------------------+  +------------------------+
```

Màn hình lỗi có icon, tiêu đề, lý do và hành động cụ thể. Camera dừng khi chuyển trang; khi quét thành công/đang xác thực thì khóa quét frame mới. Kết quả được lấy từ server, âm/rung là bổ trợ tùy thiết bị; không dựa vào âm/rung để xác nhận.

### 5.5. Admin queue & detail

```text
+------------------------------------------------------+
| Logo                        Admin workspace / User  |
|------+-----------------------------------------------|
| Side | Approval / Exceptions / Settlements           |
| nav  | [Search] [Status] [Date] [Other filters]      |
|      | ID | Name | Status | Submitted | Action       |
|      | ...                              [Review]     |
|      | Pagination                                    |
|      | Detail: Evidence | Related record | Timeline  |
|      | Reason / Reference          [Reject][Approve] |
+------------------------------------------------------+
```

Trên desktop detail có thể dùng drawer rộng hoặc page; với tài chính/kiểm duyệt phức tạp ưu tiên page để giữ context. Mobile dùng full-screen, bảng cuộn ngang. Các nút nguy hiểm tách khỏi primary action thông thường.

## 6. State system, data formatting & interaction contracts

### 6.1. Business status labels

| Domain | Status → nhãn VI |
|---|---|
| Organizer | PENDING_VERIFICATION → Chờ xác minh; VERIFIED → Đã xác minh; REJECTED → Bị từ chối; SUSPENDED → Tạm khóa |
| Event | DRAFT → Bản nháp; PENDING_REVIEW → Chờ duyệt; APPROVED → Đã duyệt; REJECTED → Bị từ chối; PUBLISHED → Đã công khai; ONGOING → Đang diễn ra; COMPLETED → Đã kết thúc; CANCELLED → Đã hủy |
| Order | PENDING_PAYMENT → Chờ thanh toán; CONFIRMED → Đã xác nhận; EXPIRED → Hết hạn; CANCELLED → Đã hủy |
| Payment | PENDING → Đang xử lý; SUCCESS → Thành công; FAILED → Thất bại; EXPIRED → Hết hạn; REFUNDED → Đã hoàn trả |
| Reservation | ACTIVE → Đang giữ vé; CONSUMED → Đã sử dụng giữ chỗ; EXPIRED → Hết hạn; RELEASED → Đã giải phóng |
| Ticket | ISSUED → Có hiệu lực (nếu sự kiện cho phép); CHECKED_IN → Đã sử dụng; VOIDED → Vô hiệu |
| Settlement | PENDING → Chờ xử lý; PROCESSING → Đang xử lý; SETTLED → Đã đối soát; FAILED → Thất bại |

Derived UI states (không tạo enum DB): sale not started / on sale / sold out / paused / closed, ticket issuance pending/error, stale data, offline, unknown payment. Chỉ backend quyết định tính hợp lệ; event đã CANCELLED luôn ngừng bán và check-in. Payment SUCCESS không đồng nghĩa vé đã được phát hành.

### 6.2. Common screen states

| State | UI contract |
|---|---|
| Initial / loading | Skeleton đúng cấu trúc; aria-busy; không hiện KPI 0 khi chưa tải |
| Empty | Giải thích chưa có dữ liệu và CTA phù hợp quyền; no results có Reset filters |
| Error | Thông báo cụ thể, Retry đối với truy vấn; giữ input; mã hỗ trợ nếu có |
| Mutation pending | Disable submit liên quan; không optimistic success cho tiền, ticket, check-in |
| Success | Server-confirmed, cập nhật state và announce; toast nếu không quan trọng |
| Stale/conflict | Hiển thị dữ liệu đã đổi, tải lại, yêu cầu xác nhận nếu price/quantity đổi |
| Offline/timeout | Banner hoặc inline; chỉ xem cached info nếu có, không xác nhận mutation; status unknown khi request có thể đã thực hiện |
| Unauthorized | Guard, 403/404; không render dữ liệu nhạy cảm; không cache dùng chung |

### 6.3. Data and interaction conventions

- VND không lẻ, format vi-VN (ví dụ 1.500.000 ₫); free hiển thị Miễn phí. Giá từ lấy min hợp lệ; không có dữ liệu thì không hiện 0 ₫. Backend cung cấp breakdown và snapshot.
- Hiển thị ngày giờ `dd/MM/yyyy HH:mm` và timezone sự kiện. Countdown đồng bộ thời gian server, không tự reset khi reload/chuyển tab. Tại 0, disable pay và fetch trạng thái; nếu đã thanh toán thì theo kết quả backend.
- List dùng filter/search/sort/page qua URL; thay đổi filter reset page, giữ bộ lọc khi back. Số bản ghi/page đề xuất 20 (10 mobile), chọn 20/50/100 desktop tùy API.
- Không hiển thị full bank account hoặc tài liệu xác minh ở list, chỉ masked. Detail chỉ người có quyền; không log token QR, mật khẩu hoặc dữ liệu thanh toán nhạy cảm. Không đặt email/PII trong URL.
- Mọi mutation quan trọng phải có response xác nhận; lỗi field từ server map đúng trường, lỗi chung hiển thị ở đầu form. Nếu timeout không suy diễn thất bại; reload/query trước khi retry.
- Quy tắc nhập: trim text, email hợp lệ, số nguyên không âm cho giá/số lượng, bắt buộc lý do reject/cancel và reference cho giao dịch tài chính. Độ dài, format, file type/size, check-in window do backend contract xác định; gợi ý ở UI không thay validation server.

## 7. Screen specifications

49 màn hình nghiệp vụ theo PRD và 2 màn hình xác thực bổ trợ. Mỗi mục có route, mục đích, layout, dữ liệu/controls, actions, states và acceptance. Shared states ở mục 6 áp dụng cho toàn bộ màn hình. ID màn hình ổn định để dùng cho backlog, Figma và test. Detail drawer, dialog, tabs và wizard steps được mô tả trong màn hình cha, không tính thành page độc lập.

### 7.1. Customer Application — 15 màn hình

#### C-01 — Home Page

**Route:** `/` · **Access:** Guest / Customer

**Layout:** Hero, search, danh mục, featured/popular/upcoming grids, footer; mobile bottom nav.

**Data & controls:** Tên/ảnh/ngày/địa điểm/giá từ/trạng thái từng event card; danh mục, search input.

**Actions & navigation:** Search → C-03; category → C-02; card → C-04; login / My Tickets.

**Validation & states:** Skeleton từng section; không có featured thì ẩn section, không dùng sự kiện giả; banner lỗi tải.

**Acceptance:** Guest khám phá không cần đăng nhập, chỉ hiển thị sự kiện public được backend cho phép.

**PRD mapping:** DSC-01–04, DSC-09

#### C-02 — Event Listing

**Route:** `/events` · **Access:** Guest / Customer

**Layout:** Header, breadcrumb, filter sidebar desktop / filter sheet mobile, grid kết quả, pagination.

**Data & controls:** Category, tỉnh/thành, date range, min/max price; sort ngày/giá nếu API hỗ trợ; số kết quả và filter chips.

**Actions & navigation:** Apply/reset; đổi trang; click card → C-04; search → C-03.

**Validation & states:** Date range và price range hợp lệ; empty với reset; giữ bộ lọc khi back; lỗi retry.

**Acceptance:** Kết quả và URL phản ánh filters; không hiển thị event chưa công khai.

**PRD mapping:** DSC-05–09

#### C-03 — Search Results

**Route:** `/events/search?q=...` · **Access:** Guest / Customer

**Layout:** Search bar ở trên, kết quả dạng grid; dùng chung filter component C-02.

**Data & controls:** Từ khóa, số kết quả, category/location/date/price, pagination; từ khóa đã áp dụng.

**Actions & navigation:** Enter/search; clear; filter; click C-04.

**Validation & states:** Trim q; q rỗng → C-02; hủy response cũ khi query thay đổi; no results có gợi ý sửa từ khóa.

**Acceptance:** Reload/back giữ query và filters; không trộn kết quả query cũ.

**PRD mapping:** DSC-04–09

#### C-04 — Event Details

**Route:** `/events/:eventId` · **Access:** Guest / Customer

**Layout:** Banner, title/date/venue/organizer, description, terms, ticket list; sticky booking summary desktop/mobile.

**Data & controls:** Tên, ảnh, danh mục, địa chỉ, timezone, mô tả, hạng vé, giá, sale windows, trạng thái, thông tin organizer.

**Actions & navigation:** Mua vé → C-08; xem hạng vé; quay về listing.

**Validation & states:** Not started/paused/sold out/closed: CTA disabled + lý do; cancelled: thông báo nổi bật; private/not found → guard.

**Acceptance:** Giá từ và trạng thái dựa trên server; không tạo reservation khi chỉ xem.

**PRD mapping:** DSC-09–11; BR-02–03, BR-20

#### C-05 — Login

**Route:** `/auth/login` · **Access:** Guest / session expired

**Layout:** Form card giữa, logo, link đăng ký/quên mật khẩu; không sidebar.

**Data & controls:** Email, password (toggle visibility), return path nội bộ; lỗi field.

**Actions & navigation:** Đăng nhập → return path hoặc home/workspace; C-06; C-07.

**Validation & states:** Generic invalid credentials; khóa tài khoản; rate limit; email chưa xác minh → X-02; không giữ password khi reload.

**Acceptance:** Không redirect ra domain tùy ý; authentication/role lấy từ server.

**PRD mapping:** AUTH-02, AUTH-09–11

#### C-06 — Register

**Route:** `/auth/register` · **Access:** Guest

**Layout:** Form card, logo, link điều khoản và login.

**Data & controls:** Tên hiển thị, email, mật khẩu, xác nhận mật khẩu (đề xuất), đồng ý điều khoản nếu được yêu cầu.

**Actions & navigation:** Đăng ký → X-02 / verification instructions; C-05.

**Validation & states:** Kiểm tra email, password policy từ backend, mismatch; email đã tồn tại xử lý tránh lộ account; loading khi submit.

**Acceptance:** Không tự cấp Organizer/Admin; đăng ký thành công dẫn xác minh email.

**PRD mapping:** AUTH-01, AUTH-04; Email registration

#### C-07 — Forgot Password

**Route:** `/auth/forgot-password` · **Access:** Guest

**Layout:** Form nhỏ với hướng dẫn, email và trạng thái đã gửi.

**Data & controls:** Email, nút gửi, link login.

**Actions & navigation:** Submit yêu cầu reset; quay C-05.

**Validation & states:** Phản hồi chung dù email không tồn tại; cooldown/resend; validate email.

**Acceptance:** Không tiết lộ account existence; reset link đi X-01.

**PRD mapping:** AUTH-05, AUTH-06

#### C-08 — Ticket Selection

**Route:** `/booking/:eventId` · **Access:** Guest / Customer; reserve cần auth

**Layout:** Stepper 1/3, ticket cards và sticky summary; mobile stepper lớn.

**Data & controls:** Ticket type, unit price, remaining nếu API cho phép, limit per order, quantity, subtotal/total dự kiến.

**Actions & navigation:** +/-; tiếp tục → C-05 nếu guest (giữ lựa chọn, không giữ vé); đã login → server reserve/create order → C-09.

**Validation & states:** Min 0; ít nhất 1 vé; không vượt limit/available; stale price/stock refresh và xác nhận lại; sold out giữ lựa chọn còn hợp lệ.

**Acceptance:** Một event nhiều ticket type; chỉ backend giữ chỗ nguyên tử; không tự trừ kho.

**PRD mapping:** INV-07–13, ORD-01–04, ORD-15; BR-05

#### C-09 — Checkout

**Route:** `/checkout/:orderId` · **Access:** Customer / owner

**Layout:** Stepper 2/3, server timer, thông tin liên hệ, order summary, payment method, sticky CTA.

**Data & controls:** Order ID, event, quantities, price snapshot, fees nếu có, total, expiresAt, tên/email người mua; đồng ý điều khoản.

**Actions & navigation:** Thanh toán → gateway → C-10; total=0 → confirm free; hủy pending qua dialog; xem đơn.

**Validation & states:** Timer hết hạn: vô hiệu pay, query order; pending payment: khóa retry/hủy nếu backend không cho; failed: retry có điều kiện; giá không đổi từ order snapshot.

**Acceptance:** Không dùng giá tự tính thay server, double click không tạo giao dịch trùng; không lưu dữ liệu thẻ.

**PRD mapping:** ORD-05–12, PAY-01–09; BR-06–09, BR-14, BR-19, BR-25

#### C-10 — Payment Result

**Route:** `/payment/result/:orderId` · **Access:** Customer / owner

**Layout:** Stepper 3/3 hoặc status page, icon, explanation, order reference, next action.

**Data & controls:** Order status, payment status, issuance state, updated at, support reference nếu exception.

**Actions & navigation:** Kiểm tra trạng thái; xem đơn C-12; nếu vé sẵn → C-13; retry payment chỉ khi server cho.

**Validation & states:** PENDING / UNKNOWN không báo fail hay success; SUCCESS + CONFIRMED → issuing/ready; EXPIRED + late SUCCESS → ngoại lệ; failed → hướng dẫn; poll bounded, manual refresh.

**Acceptance:** Return params không xác nhận thành công; không khởi tạo lại order khi refresh.

**PRD mapping:** PAY-04–13, ETK-01; BR-08–09, BR-22–23

#### C-11 — My Orders

**Route:** `/account/orders` · **Access:** Customer / owner

**Layout:** Account nav, list/table, tabs All / Pending / Confirmed / Expired / Cancelled.

**Data & controls:** Order ID, event, booked date, total, qty, order status và payment status riêng; date/status filter.

**Actions & navigation:** Mở C-12; pending còn hiệu lực → C-09; tìm sự kiện → C-02.

**Validation & states:** Empty có CTA khám phá; no results/reset; stale status refresh; không lộ đơn khác.

**Acceptance:** Chỉ đơn của user; filter/order labels nhất quán PRD.

**PRD mapping:** ORD-13–14, PAY-10; BR-18

#### C-12 — Order Details

**Route:** `/account/orders/:orderId` · **Access:** Customer / owner

**Layout:** Header order/status, summary, ticket items, payment timeline và support notice.

**Data & controls:** Order ID, event, snapshot price, qty, total, paid time, masked transaction ref, status, issuance progress.

**Actions & navigation:** Pay tiếp nếu eligible; hủy pending nếu được phép; xem vé C-14/C-13; refresh; contact support (kênh đã cấu hình).

**Validation & states:** CONFIRMED đang phát hành: thông báo; EXPIRED/CANCELLED: không pay; ngoại lệ late payment: thông báo riêng; không CTA refund.

**Acceptance:** Đơn confirmed không tự hủy; không hiển thị chi tiết thẻ hay internal audit.

**PRD mapping:** ORD-09–14, PAY-10–13, ETK-01; BR-08, BR-14

#### C-13 — My Tickets

**Route:** `/account/tickets` · **Access:** Customer / owner

**Layout:** Account nav, tabs Sắp tới / Đã qua / Vô hiệu, nhóm theo event, mỗi ticket card có status.

**Data & controls:** Event, date, venue, ticket ID, type, status; issuance pending hiển thị qua link order nếu backend cung cấp.

**Actions & navigation:** Mở C-14; tìm thêm event; xem đơn.

**Validation & states:** Không có vé: CTA khám phá; chưa phát hành: tra cứu order; cancelled/voided rõ ràng; không QR trên list.

**Acceptance:** Chỉ vé cá nhân; một item cho mỗi vé; không gộp QR nhiều người.

**PRD mapping:** ETK-04, ETK-08–10

#### C-14 — Ticket Details

**Route:** `/account/tickets/:ticketId` · **Access:** Customer / owner

**Layout:** E-ticket card, event/date/venue, QR lớn, ticket info, action download, back.

**Data & controls:** Ticket ID, type, attendee hoặc buyer, event, ticket status, check-in time nếu có, QR cho ISSUED hợp lệ.

**Actions & navigation:** Hiển thị QR; tải PDF; xem C-12; back.

**Validation & states:** VOIDED/cancelled: không hiện QR sử dụng, PDF nếu cho phép phải đánh dấu vô hiệu; CHECKED_IN: rõ đã dùng; lỗi tải PDF retry.

**Acceptance:** QR không có PII thô, không đưa token vào URL; PDF/status nhất quán server; một QR riêng mỗi vé.

**PRD mapping:** ETK-02–09; BR-10–12, BR-20

#### C-15 — User Profile

**Route:** `/account/profile` · **Access:** Authenticated owner

**Layout:** Account sidebar/tabs thông tin cá nhân, bảo mật; mobile stacked.

**Data & controls:** Tên, email (read-only trừ khi flow đổi email được duyệt), thông tin liên hệ nếu có; current/new/confirm password.

**Actions & navigation:** Lưu hồ sơ; đổi mật khẩu; logout; đăng ký Organizer → O-01; chọn workspace có quyền.

**Validation & states:** Dirty form confirm; mismatch/password policy; không lưu password; logout reset private cache.

**Acceptance:** Không thể đổi role tự phục vụ; email không tự đổi nếu thiếu quy trình xác minh.

**PRD mapping:** AUTH-03, AUTH-07–08, AUTH-10–11, ORG-01

### 7.2. Organizer Portal — 14 màn hình

#### O-01 — Organizer Registration

**Route:** `/organizer/register` · **Access:** Customer / applicant

**Layout:** Onboarding form và status view, sections thông tin tổ chức / xác minh / payout (khi cần).

**Data & controls:** Tên tổ chức, loại cá nhân/tổ chức, liên hệ, địa chỉ, tài liệu xác minh theo policy, bank fields nếu yêu cầu; status và rejection reason.

**Actions & navigation:** Save/submit hồ sơ theo API; resubmit nếu rejected; verified → O-02.

**Validation & states:** PENDING read-only hoặc theo policy; rejected editable; suspended thông báo; file upload errors; mask bank info.

**Acceptance:** User chưa VERIFIED không submit event; không tự cấp role; lý do và trạng thái từ backend.

**PRD mapping:** ORG-01–05, ORG-09; BR-01

#### O-02 — Organizer Dashboard

**Route:** `/organizer/:orgId/dashboard` · **Access:** Organizer / org member

**Layout:** Portal shell, KPI row, sales chart, event list, task checklist.

**Data & controls:** Date range, event count, sold/available, gross/fees/net, check-in, unresolved settlement info; last updated.

**Actions & navigation:** Create event O-04; event O-06; analytics O-12; settlement O-13.

**Validation & states:** Chưa có event: CTA tạo; nonverified: onboarding banner; no data không giả 0; filter loading.

**Acceptance:** KPI đúng organization; period/scope rõ ràng, nguồn server.

**PRD mapping:** Reporting; ORG-09, EVT-01; BR-18

#### O-03 — My Events

**Route:** `/organizer/:orgId/events` · **Access:** Organizer / member

**Layout:** Header Create event, tabs/filter/search, table desktop/cards mobile.

**Data & controls:** Name/banner thumbnail, date, status, sales window summary, sold/capacity, last update.

**Actions & navigation:** New O-04; view O-06; edit O-05 nếu allowed; filter/status.

**Validation & states:** Không có sự kiện; pending/rejected labels; không physical delete có giao dịch; denied actions lý do.

**Acceptance:** Chỉ sự kiện trong org; filter/back giữ query; action theo allowed transitions.

**PRD mapping:** EVT-01–13; BR-01–02, BR-13, BR-18

#### O-04 — Create Event

**Route:** `/organizer/:orgId/events/new` · **Access:** Organizer / permitted

**Layout:** 4-step wizard: Basic / Venue & time / Tickets / Review, draft saved status, preview.

**Data & controls:** Tên, category, description, banner; venue/address, start/end/timezone, capacity, terms; ticket type/price/qty/sales dates/limit; review public preview.

**Actions & navigation:** Save draft, next/back, add/remove ticket draft, submit review, return O-06.

**Validation & states:** End > start; capacity positive; tổng quota ≤ capacity; price ≥0; sale start < end và không ngoài window được backend cho; upload retry; save conflict.

**Acceptance:** Draft giữ server ID; submit chỉ VERIFIED với dữ liệu hợp lệ; không tự publish.

**PRD mapping:** EVT-01–09, INV-01–07; BR-01, BR-04

#### O-05 — Edit Event

**Route:** `/organizer/:orgId/events/:eventId/edit` · **Access:** Organizer / permitted

**Layout:** Dùng lại wizard/form, current status, locked fields, change summary.

**Data & controls:** Existing basic/venue/time/capacity/terms; ticket config link O-07; rejection reason; last updated.

**Actions & navigation:** Save draft/changes; resubmit nếu rejected; cancel edits; preview.

**Validation & states:** Dirty warning, stale version conflict; khóa sửa trọng yếu khi đã bán (đề xuất, cần policy); pending review limitations.

**Acceptance:** Không giảm capacity trái quy tắc, thay đổi không tác động order snapshot; không tự publish sau sửa.

**PRD mapping:** EVT-02–08, EVT-13; BR-04, BR-14, BR-21

#### O-06 — Event Details / Overview

**Route:** `/organizer/:orgId/events/:eventId` · **Access:** Organizer / permitted

**Layout:** Event header, status, tabs, publish checklist, KPI, event metadata, lifecycle history.

**Data & controls:** Name/banner/status/review feedback, date/venue, ticket capacity/sold/reserved, order/check-in totals, publish URL khi công khai.

**Actions & navigation:** Edit O-05; submit review; publish APPROVED; cancel event có confirm/reason; tabs O-07–11.

**Validation & states:** Cancel hiển thị impact giao dịch và nghĩa vụ tài chính; reject có lý do; action chỉ khi server allowed; stale state refresh.

**Acceptance:** Approve không tự publish; cancelled ngừng bán/check-in; audit và notice sau xác nhận.

**PRD mapping:** EVT-09–14; BR-02, BR-13, BR-20

#### O-07 — Ticket Type Management

**Route:** `/organizer/:orgId/events/:eventId/tickets` · **Access:** Organizer / permitted

**Layout:** Capacity progress, ticket table, add/edit drawer, sale status badges.

**Data & controls:** Name/description, price, quota, reserved/sold/available, sale start/end, max/order, status.

**Actions & navigation:** Add/edit, pause/resume, filter; confirm risky changes.

**Validation & states:** Quota ≥ sold+reserved; total quota ≤ event capacity; price nonnegative integer; ongoing sales price change snapshot unaffected; backend conflict refresh.

**Acceptance:** Không decrease dưới committed inventory; không sửa trực tiếp sold/reserved; trạng thái server.

**PRD mapping:** INV-01–13; BR-03–06, BR-14, BR-21

#### O-08 — Order Management

**Route:** `/organizer/:orgId/events/:eventId/orders` · **Access:** Organizer / permitted

**Layout:** Filterable order table, detail drawer/read-only timeline.

**Data & controls:** Order ID, buyer masked/contact theo quyền, qty, amount snapshot, order/payment status, date; detail items.

**Actions & navigation:** Search order ID, filter date/status, open detail, link attendees nếu allowed.

**Validation & states:** Không có order; pending/exception phân biệt; không có sửa giá, manual confirm payment hoặc refund button.

**Acceptance:** Dữ liệu đúng event; PII tối thiểu; no customer payment mutation.

**PRD mapping:** ORD-13–14, PAY-10–11; BR-14, BR-18

#### O-09 — Attendee Management

**Route:** `/organizer/:orgId/events/:eventId/attendees` · **Access:** Organizer / permitted

**Layout:** Table per ticket, search/filter, detail drawer.

**Data & controls:** Ticket ID, buyer/attendee, type, issued/check-in status, order ref, time; totals.

**Actions & navigation:** Search theo ID/name nếu API hỗ trợ; filter ticket type/status; view ticket record/order.

**Validation & states:** Pending issuance không tính đã phát hành; voided rõ; không hiện raw QR hoặc action đổi owner.

**Acceptance:** Một row/ticket; quyền event scope; không phát hành thủ công/double issue.

**PRD mapping:** EVT-14, ETK-08, CHK-10; BR-10–12, BR-18

#### O-10 — Check-in Dashboard

**Route:** `/organizer/:orgId/events/:eventId/check-in` · **Access:** Organizer / permitted

**Layout:** Stats cards, attendance progress, breakdown type, recent check-ins table.

**Data & controls:** Issued eligible, checked-in, remaining eligible, ratio, time and staff; refreshed at.

**Actions & navigation:** Refresh, filter ticket type/time, view history; link assigned scanner nếu có quyền.

**Validation & states:** Offline/stale banner; empty before event; zero denominator → N/A; cancelled stop entry.

**Acceptance:** Chỉ successful check-ins tính attendance; không tạo check-in từ dashboard không có quyền.

**PRD mapping:** CHK-09–10, Reporting; BR-11, BR-20

#### O-11 — Staff Management

**Route:** `/organizer/:orgId/staff; /organizer/:orgId/events/:eventId/staff` · **Access:** Organizer / member admin

**Layout:** List members, role/membership, add user / assignment drawer, event context tab.

**Data & controls:** User email/ID, name, membership status/role, assigned events, event staff list.

**Actions & navigation:** Add/remove member hoặc staff theo API; assign/unassign event; confirm revoke.

**Validation & states:** Không tìm thấy user, duplicate assignment, unauthorized, cannot remove last allowed owner theo policy cần chốt; stale assignment refresh.

**Acceptance:** Staff chỉ truy cập assigned event; revoke có hiệu lực server; không cho tự cấp ADMIN.

**PRD mapping:** ORG-06–08; BR-18

#### O-12 — Revenue & Analytics

**Route:** `/organizer/:orgId/analytics` · **Access:** Organizer / permitted

**Layout:** Date/event filters, KPI cards, revenue trend, sales by type, check-in ratio, CSV export.

**Data & controls:** Gross, platform fee, gateway fee, adjustments, net; sold, available, capacity, settled amount; time series.

**Actions & navigation:** Đổi period/event; export CSV dữ liệu cùng filters; drill to O-06/O-13.

**Validation & states:** Gateway fee chưa xác định hiển thị pending/estimate rõ, no data N/A; export error/retry.

**Acceptance:** Số liệu nhất quán định nghĩa mục 8; export đúng scope; không ẩn adjustments.

**PRD mapping:** Reporting, STL-04–07, STL-12; BR-18

#### O-13 — Settlement Management

**Route:** `/organizer/:orgId/settlements` · **Access:** Organizer / permitted finance

**Layout:** List, eligible event CTA, settlement detail drawer/route, status timeline.

**Data & controls:** Event, gross/fee/adjustments/net, eligibility reasons, payout account masked, ref/date/status; item details.

**Actions & navigation:** Request settlement nếu eligible; view detail/history; refresh, export nếu thuộc báo cáo CSV.

**Validation & states:** Event chưa kết thúc, pending exception/fee unknown, existing request: CTA disabled lý do; failed show support/next step.

**Acceptance:** Không chuyển tiền từ UI Organizer; một khoản chỉ đối soát một lần; request thành công theo server.

**PRD mapping:** STL-05–12; BR-16–17, BR-24

#### O-14 — Organization Settings

**Route:** `/organizer/:orgId/settings` · **Access:** Organizer / permitted

**Layout:** Tabs General / Payout / Verification; form sections; status header.

**Data & controls:** Tên, address, contact, verification state/documents theo quyền, masked bank account/name/bank; edit permitted fields.

**Actions & navigation:** Save general; update payout có confirmation; xem status O-01 nếu phù hợp.

**Validation & states:** High-risk changes cảnh báo, mask data, server reverify theo policy; form dirty; không sửa role tự do.

**Acceptance:** Không lưu full bank account vào logs/cache; thay đổi payout không thay historical settlement snapshot.

**PRD mapping:** ORG-02–05, ORG-09; BR-17–18

### 7.3. Check-in Application — 6 màn hình

#### S-01 — Assigned Events

**Route:** `/check-in/events` · **Access:** Check-in Staff / assigned Organizer

**Layout:** Mobile list, event cards, current/upcoming/ended groups, connection marker.

**Data & controls:** Event title/date/venue, check-in window, assignment status, allowed flag.

**Actions & navigation:** Choose eligible → S-02; refresh; switch workspace.

**Validation & states:** Empty assignment contact organizer; expired/removed assignment; cancelled/closed event không scan.

**Acceptance:** Chỉ assigned event; không chọn event bằng ID để vượt quyền.

**PRD mapping:** CHK-01, CHK-05; ORG-08, BR-18, BR-20

#### S-02 — QR Code Scanner

**Route:** `/check-in/events/:eventId/scan` · **Access:** Assigned staff

**Layout:** Camera viewport, bounding frame, title/status connection, CTA lookup, bottom nav.

**Data & controls:** Camera device permission/state, scan progress, selected event, online indicator.

**Actions & navigation:** Scan QR → server validate+check-in → S-03; camera switch/torch nếu supported; lookup S-04.

**Validation & states:** Permission denied/no camera → lookup; decode fail scan again; pending locks repeated frames; offline stop; timeout unknown verify before retry.

**Acceptance:** Không check-in client-side; camera stop khi rời màn hình, không log QR token; chỉ một successful scan.

**PRD mapping:** CHK-02–06; BR-10–12, BR-20

#### S-03 — Ticket Validation Result

**Route:** `/check-in/events/:eventId/result` · **Access:** Assigned staff

**Layout:** Full-screen result hoặc overlay, icon/text lớn, ticket info, next CTA.

**Data & controls:** Result code, ticket ID/type/name nếu được phép, time, staff, event context; lý do lỗi.

**Actions & navigation:** Scan next → S-02; manual lookup → S-04; query status khi unknown.

**Validation & states:** SUCCESS; ALREADY_USED (thời gian trước nếu được phép); INVALID; WRONG_EVENT; VOIDED; OUTSIDE_WINDOW; UNAUTHORIZED; UNKNOWN_NETWORK. Không hiển thị success nếu chưa xác nhận.

**Acceptance:** Duplicate không bị diễn giải là admission mới; reload không replay mutation, no PII/QR in URL.

**PRD mapping:** CHK-03–06, CHK-09; BR-11–12, BR-20

#### S-04 — Manual Ticket Lookup

**Route:** `/check-in/events/:eventId/lookup` · **Access:** Assigned staff

**Layout:** Search form, result card, explicit confirm action; input keyboard-friendly.

**Data & controls:** Ticket ID chính xác; thông tin tối thiểu, validity, check-in state; không tìm theo PII nếu không được duyệt.

**Actions & navigation:** Lookup read-only; confirm check-in nếu eligible → S-03; clear/back.

**Validation & states:** Not found generic, wrong event, already used, outside window; offline no action; double submit lock.

**Acceptance:** Lookup không thay đổi trạng thái; manual confirm dùng cùng backend rule như scan.

**PRD mapping:** CHK-07–08; BR-11–12, BR-18, BR-20

#### S-05 — Check-in History

**Route:** `/check-in/events/:eventId/history` · **Access:** Assigned staff

**Layout:** Mobile list/table, date/time filter, refresh control.

**Data & controls:** Ticket ID, type, result (nếu API có), time, staff theo quyền; no raw token.

**Actions & navigation:** Refresh; filter/search theo ticket ID; open allowed detail.

**Validation & states:** Empty, stale/offline rõ, no edit/undo; history theo quyền.

**Acceptance:** Không tạo check-in từ history; success count không tính duplicate.

**PRD mapping:** CHK-09; BR-11, BR-18

#### S-06 — Check-in Statistics

**Route:** `/check-in/events/:eventId/stats` · **Access:** Assigned staff

**Layout:** KPI lớn, progress, ticket type breakdown, updated timestamp.

**Data & controls:** Eligible issued, checked-in, còn lại, occupancy ratio, counts by type.

**Actions & navigation:** Refresh; đổi event qua S-01; back scanner.

**Validation & states:** Stale/no network; no denominator N/A; cancelled no scan CTA.

**Acceptance:** Số liệu server; không ước lượng từ local scans, tránh leakage event khác.

**PRD mapping:** CHK-10; BR-18, BR-20

### 7.4. Admin Portal — 14 màn hình

#### A-01 — Admin Dashboard

**Route:** `/admin/dashboard` · **Access:** Admin

**Layout:** Portal sidebar, KPI cards, trend chart, action queues, exceptions panel.

**Data & controls:** Users, organizers, events, sold, GMV, platform fee revenue, payment status/settlement metrics, unresolved exceptions; filter period.

**Actions & navigation:** Drill to A-04/A-06/A-10/A-12; change period; refresh.

**Validation & states:** No data N/A; partial widget errors isolated; pending operational tasks rõ.

**Acceptance:** KPI định nghĩa mục 8; số liệu không gộp payment exceptions vào GMV hợp lệ.

**PRD mapping:** Reporting, ADM-03–13

#### A-02 — User Management

**Route:** `/admin/users` · **Access:** Admin

**Layout:** Search/filter table, detail drawer, lock/unlock confirmation.

**Data & controls:** User ID, masked email, display name, roles, verification state, account status/date.

**Actions & navigation:** View; lock/unlock với reason; filter role/status.

**Validation & states:** Already locked, stale state, protected admin account policy; no password or token display.

**Acceptance:** Không sửa role ngoài scope; lock/unlock audited và quyền server xác nhận.

**PRD mapping:** ADM-01–02, AUTH-10–11

#### A-03 — Organizer Management

**Route:** `/admin/organizers` · **Access:** Admin

**Layout:** List by verification & status, detail tabs profile/events/payout masked.

**Data & controls:** Org ID/name, contact masked, status, submitted date, count events; detail verification links.

**Actions & navigation:** View; open A-04; suspend/restore nếu allowed (reason); filter.

**Validation & states:** Hiện impact suspension, không giả định auto cancel events; stale review refresh.

**Acceptance:** Thông tin nhạy cảm giới hạn; status mutation audit; chỉ allowed transitions.

**PRD mapping:** ADM-03–04, ORG-09; BR-17–18

#### A-04 — Organizer Verification

**Route:** `/admin/organizers/verification; /:orgId detail` · **Access:** Admin

**Layout:** Queue pending, evidence review page, applicant profile, decision history, action area.

**Data & controls:** Org ID/name/type, submission time, verification docs, masked payout/account, prior decisions.

**Actions & navigation:** Approve / Reject (reason mandatory reject); preview docs an toàn; next queue item.

**Validation & states:** Missing docs: reject with reason hoặc giữ pending; already processed conflict; server errors preserve reason.

**Acceptance:** Chỉ PENDING_VERIFICATION xử lý; email quyết định theo server; không tự publish events.

**PRD mapping:** ADM-04, ORG-03–04; BR-01

#### A-05 — Event Management

**Route:** `/admin/events` · **Access:** Admin

**Layout:** Event list, filters, detail page/drawer, featured control.

**Data & controls:** Event ID/title, org, category, status, schedule, capacity/sold, featured flag; detail lifecycle.

**Actions & navigation:** View, open A-06; toggle featured nếu public eligible; quản lý event action theo policy.

**Validation & states:** Cancelled/hidden không featured; no physical delete nếu giao dịch; stale/impact confirm.

**Acceptance:** Admin không bypass event approval; featured thay đổi audited; no arbitrary manual booking.

**PRD mapping:** ADM-06–08; BR-02, BR-13, BR-20

#### A-06 — Event Approval

**Route:** `/admin/events/approval; /:eventId detail` · **Access:** Admin

**Layout:** Queue, event preview, ticket config, organizer verification, review checklist, decision timeline.

**Data & controls:** Title/banner/description/category, venue/time/terms, capacity, ticket prices/limits/windows, organizer status.

**Actions & navigation:** Approve/reject với lý do reject; view organizer; back pending queue.

**Validation & states:** Missing/invalid data: reject/keep pending; concurrent moderator decision conflict; pending only.

**Acceptance:** Approve chuyển APPROVED, không tự publish; decision email và audit, verify org first.

**PRD mapping:** ADM-07, EVT-09–11; BR-01–04

#### A-07 — Category Management

**Route:** `/admin/categories` · **Access:** Admin

**Layout:** Table, create/edit modal, active state management nếu backend hỗ trợ.

**Data & controls:** Category ID/name VI/EN, slug hoặc code, active state, associated event count.

**Actions & navigation:** Create/edit; deactivate nếu supported; filter/search.

**Validation & states:** Unique code/name policy; referenced category không xóa vật lý (đề xuất); validate fields; conflict.

**Acceptance:** Không phá reference event cũ; chỉ category active cho event mới theo policy.

**PRD mapping:** ADM-05, EVT-04, DSC-05

#### A-08 — Order Management

**Route:** `/admin/orders` · **Access:** Admin

**Layout:** Search/filter table, detail with items, reservation, payment & ticket links.

**Data & controls:** Order ID, event/org, buyer masked, qty, amount, statuses, created/expiresAt, transaction refs.

**Actions & navigation:** View detail; link A-09/A-10; filter status/time/IDs.

**Validation & states:** Pending vs expired vs confirmed phân biệt; late payment flag; không manual confirm/order edit.

**Acceptance:** Read-only financial facts; không override snapshot hoặc reservation state.

**PRD mapping:** ADM-09, ORD-05–14; BR-06–09, BR-14

#### A-09 — Payment Management

**Route:** `/admin/payments` · **Access:** Admin

**Layout:** Transaction table, reconciliation view/detail, status timeline.

**Data & controls:** Payment/transaction ID, order, amount, currency, provider ref, status, update time, reconcile difference; masked metadata.

**Actions & navigation:** Search/filter; query provider/reconcile theo backend; open exception A-10; view linked order.

**Validation & states:** Unknown/pending, mismatch, duplicate callback, provider error; no auto retry money; no card data.

**Acceptance:** Payment SUCCESS không downgrade bởi failure đến muộn; reconciliation có audit.

**PRD mapping:** ADM-10, PAY-10–13; BR-09, BR-23, BR-25

#### A-10 — Payment Exception Management

**Route:** `/admin/payment-exceptions` · **Access:** Admin

**Layout:** Work queue, detail case, related payment/order, evidence and resolution form.

**Data & controls:** Exception ID/type, expected vs received amount, order/payment/event ref, status, reason, owner nếu backend hỗ trợ, timeline, external refund ref.

**Actions & navigation:** Review, record handling progress, confirm manual resolution với reference/amount/date/evidence; refresh.

**Validation & states:** Late success/duplicate amount/cancelled event; missing evidence block; unresolved block settlement; conflict nếu đã resolved.

**Acceptance:** Không có nút automated refund; chỉ xác nhận khoản thực tế đã hoàn; audit, không double resolve.

**PRD mapping:** ADM-11, PAY-13, BR-16–17, BR-20, BR-24

#### A-11 — Commission Configuration

**Route:** `/admin/commissions` · **Access:** Admin

**Layout:** Current fee summary, edit form, effective/snapshot note, history.

**Data & controls:** Fee type PERCENTAGE hoặc FIXED_AMOUNT; value, currency/unit, effective time nếu contract hỗ trợ, changed by/time.

**Actions & navigation:** Edit → preview effect on example → confirm save; view config history.

**Validation & states:** Percent 0–100, fixed ≥0; quy tắc cap/net âm cần chốt; effective date, concurrent update; không hybrid.

**Acceptance:** Thay đổi chỉ ảnh hưởng giao dịch tương lai theo snapshot policy; audit; không sửa giá cũ.

**PRD mapping:** ADM-12, STL-01–05; BR-15, BR-17

#### A-12 — Settlement Management

**Route:** `/admin/settlements` · **Access:** Admin

**Layout:** Queue list, detailed settlement breakdown, eligibility panel, action/timeline.

**Data & controls:** Settlement ID/event/org, gross/fees/adjustments/net, items, payout masked/verified, statuses, exception count, transfer reference/date.

**Actions & navigation:** Review eligible, mark processing, record manual transfer + confirm SETTLED, record FAILED reason; reopen theo policy an toàn.

**Validation & states:** Không settle trước COMPLETED hoặc unresolved exception/unknown fee; timeout kiểm tra trạng thái/reference; duplicate reject.

**Acceptance:** Không automated payout; bằng chứng trước SETTLED; prevent duplicate settlement và audit.

**PRD mapping:** ADM-13, STL-08–12; BR-16–17, BR-24

#### A-13 — Audit Logs

**Route:** `/admin/audit-logs` · **Access:** Admin

**Layout:** Search/filter table, detail read-only timeline, structured changes redacted.

**Data & controls:** Actor ID, action, resource type/ID, timestamp, outcome, correlation ID nếu có, redacted before/after.

**Actions & navigation:** Filter actor/action/resource/time; detail; copy safe reference.

**Validation & states:** No results/reset; access restriction; export không mặc định scope; không delete/edit audit.

**Acceptance:** Không hiển thị password, QR token, full bank/card; log tài chính và moderation truy vết được.

**PRD mapping:** ADM-14; BR-17, BR-25

#### A-14 — System Settings

**Route:** `/admin/settings` · **Access:** Admin

**Layout:** Grouped setting cards, read-only limits + editable approved keys, change history.

**Data & controls:** Reservation duration default 10 phút nếu configurable; check-in window nếu contract; support/contact config, current values; timezone/language defaults.

**Actions & navigation:** Edit từng group → preview impact → confirm; read-only for secrets; revert form not stored values.

**Validation & states:** Server validation/ranges; unknown key không editable; active reservations không reset; conflict reload.

**Acceptance:** Không đưa secrets/provider credentials UI; chỉ allowlist keys; audit mọi thay đổi quan trọng.

**PRD mapping:** ADM-15, ORD-06–10, CHK-05; BR-06, BR-17

### 7.5. Shared authentication screens — 2 màn hình bổ trợ

#### X-01 — Reset Password

**Route:** `/auth/reset-password` · **Access:** Guest with valid token

**Layout:** Form card, password rules, token status and link login.

**Data & controls:** New password, confirm, token from safe link (không log).

**Actions & navigation:** Submit reset → C-05; request new link C-07.

**Validation & states:** Expired/invalid/used token; mismatch; loading; clear token after processing.

**Acceptance:** Single-use token; generic errors; không hiển thị email nhạy cảm hoặc tự cấp session tùy tiện.

**PRD mapping:** AUTH-05–07

#### X-02 — Email Verification

**Route:** `/auth/verify-email` · **Access:** Guest / authenticated unverified

**Layout:** Status page hoặc resend form, instruction, countdown cooldown.

**Data & controls:** Verification token from link; masked email nếu authorized; verified/pending/error.

**Actions & navigation:** Process link; resend if allowed; return login / safe intended path.

**Validation & states:** Expired/used/invalid, rate-limited; already verified; không lộ account existence.

**Acceptance:** Backend xác minh token, không dựa query success; verified state cập nhật đúng.

**PRD mapping:** AUTH-04, AUTH-11; Email verification

## 8. Dashboard metrics & reporting contract

Các định nghĩa dưới đây là đề xuất UI/reporting để tránh hiểu sai số liệu; quy tắc ghi nhận/điều chỉnh chi tiết cần thống nhất với System Design và Finance trước triển khai.

| Metric | Định nghĩa / hiển thị |
|---|---|
| Tickets sold | Số vé thuộc order CONFIRMED, gồm vé miễn phí nếu áp dụng; hiển thị số vô hiệu/điều chỉnh riêng, backend quyết định inventory thực tế |
| Reserved | Số lượng giữ chỗ còn hiệu lực; không tính hold hết hạn |
| Available | Total − Reserved − Sold theo inventory authoritative; không tính bằng dữ liệu table đã phân trang |
| Sell-through | Sold / Total capacity × 100%; mẫu số 0 hiển thị N/A |
| Check-in rate | Successful unique check-ins / số vé đủ điều kiện tham dự theo backend; loại vé voided khỏi mẫu số, hiển thị N/A nếu 0 |
| Gross revenue / GMV | Giá trị vé đã xác nhận theo snapshot trong phạm vi thời gian; tách khoản collected chưa xác nhận order và các điều chỉnh hoàn trả |
| Platform fee | Tổng phí snapshot được ghi nhận, không dùng current config để tính lại lịch sử |
| Gateway fee | Phí giao dịch thực tế khi đã đối soát; nếu chưa có ghi Pending/Estimate và không đánh dấu đủ điều kiện settlement |
| Net revenue | Gross revenue − platform fee − gateway fee − applicable adjustments; break down từng khoản |
| Settled amount | Khoản có trạng thái SETTLED và reference đã ghi nhận; không tính PROCESSING |
| Payment success rate | Số payment attempts thành công / số attempts có kết quả cuối cùng trong period; loại pending và callback trùng |

Dashboard filter phải chỉ rõ period, timezone, event/organization scope, date basis (ví dụ ngày xác nhận order / ngày giao dịch / ngày check-in). Các chart tổng hợp dùng server aggregates; no data khác 0. CSV giữ filter, đơn vị, date basis và không chứa PII ngoài quyền. Phân trang không ảnh hưởng KPI tổng.

## 9. Email notification UX

Không tạo notification center hoặc template editor trong MVP. Mẫu email dùng chung header logo/title, nội dung tiếng Việt/Anh theo locale, CTA chính và footer support. Email gửi bất đồng bộ; CTA dẫn đến route nội bộ có guard, không để dữ liệu nhạy cảm trong link ngoại trừ token xác thực ngắn hạn theo cơ chế bảo mật.

| Trigger theo PRD | Subject VI minh họa | Nội dung / CTA |
|---|---|---|
| Account Registration | Chào mừng bạn đến với ETP | Tóm tắt tài khoản; xác minh email nếu cần |
| Email Verification | Xác minh địa chỉ email | Link single-use có thời hạn → X-02 |
| Password Reset | Đặt lại mật khẩu | Link single-use → X-01; bỏ qua nếu không yêu cầu |
| Organizer Approved/Rejected | Kết quả xác minh nhà tổ chức | Trạng thái, lý do nếu reject, link O-01/O-02 |
| Event Approved/Rejected | Kết quả kiểm duyệt sự kiện | Tên event, trạng thái, lý do, link O-06 |
| Order Confirmation | Đơn hàng đã được xác nhận | Mã đơn, event, total, link C-12 |
| Payment Successful | Thanh toán thành công | Mã giao dịch/đơn, số tiền; không khẳng định vé sẵn nếu chưa phát hành |
| Ticket Issued | Vé điện tử của bạn đã sẵn sàng | Event/date/venue, link C-13/C-14; PDF nếu cơ chế email hỗ trợ an toàn |
| Event Reminder | Sự kiện của bạn sắp diễn ra | Ngày giờ/địa điểm và link vé; thời gian gửi cần chốt |
| Event Cancelled | Thông báo hủy sự kiện | Tên event, thời gian, hướng dẫn xử lý khoản tiền theo quy trình; link C-12/O-06/S-01 phù hợp vai trò |
| Settlement Completed | Đối soát đã hoàn tất | Số tiền, mã đối soát/transfer reference an toàn, link O-13 |

Nếu gửi email thất bại, order/ticket không rollback, customer vẫn xem được tại app. Retry theo backend; tránh gửi trùng cùng trigger. UI không mặc định cung cấp resend vé/email logs editor ngoài PRD; hỗ trợ vận hành qua quy trình server.

## 10. Accessibility, security & performance checklist

### 10.1. Accessibility

- Mục tiêu WCAG 2.2 AA theo PRD: contrast văn bản thường ≥4.5:1, văn bản lớn ≥3:1, trạng thái/control có đủ độ tương phản; test theme thực tế thay vì mặc định token đạt chuẩn.
- Toàn bộ flow không phụ thuộc chuột: tab order hợp lý, visible focus, skip link, semantic headings, form label và accessible name; modal trap focus và trả lại sau đóng.
- Touch controls tối thiểu 44 px theo design target, spacing đủ; icon và màu đi cùng text. Check-in có thông báo bằng chữ; audio/vibration tùy chọn.
- Countdown không đọc lại mỗi giây bằng screen reader; announce mốc quan trọng và trạng thái hết hạn. Error summary liên kết tới field, status change có live region thích hợp.
- QR có alternative ticket ID/manual lookup; camera permission giải thích trước, không yêu cầu camera cho Customer. Chart có summary/table, ảnh có alt text, reduced-motion được tôn trọng.

### 10.2. Security & financial UX

- Xác minh role, ownership, membership từ backend ở mọi route/mutation; route guard chỉ là UX. Không sử dụng ẩn nút như một kiểm soát bảo mật.
- Checkout redirect đến payment gateway đã cấu hình; không lưu/hiển thị card data, không tin callback client; khi quay lại query order/payment authoritative.
- Không lưu access token/refresh token, PII hoặc QR vào log, analytics, URL không cần thiết. Cơ chế lưu token do Security Design xác định.
- Xóa private state khi logout/đổi organization; no shared cache cho dữ liệu cá nhân; không dùng mock success. Audit các quyết định moderation, account lock, fee, exception, payout.
- Confirm dialog hiển thị resource, hậu quả và next step; lý do bắt buộc theo action policy. Financial references/bằng chứng được ghi nhận đúng quyền và không nhân đôi giao dịch.

### 10.3. Performance & resilience

- Mục tiêu API P95 read ≤500 ms, booking/check-in ≤1.000 ms, issuance P95 ≤30 s theo PRD; UI không cam kết kết quả trước server. Kiểm thử tải cần profile traffic riêng.
- Lazy load image ngoài viewport, đặt kích thước ảnh tránh layout shift, tối ưu thumbnail; route-level lazy loading cho portal modules; cancel stale search/filter requests.
- Data polling có giới hạn, backoff và ngừng khi terminal/unmount; không poll vô hạn. Payment unknown có refresh thủ công và hỗ trợ bằng reference.
- Tải table theo page; loading không khóa navigation không liên quan. Check-in network state và pending mutation rõ ràng; không lưu queue offline.

## 11. Cross-screen acceptance & QA scenarios

Mọi màn hình phải có test role/ownership, happy path, loading, empty, failed query, server validation, keyboard/screen reader và responsive. Các tình huống sau là các regression gates quan trọng (không thay bộ test kỹ thuật).

| Test ID | Scenario | Kỳ vọng |
|---|---|---|
| UX-01 | Guest chọn vé rồi login | Return đúng event/selection, không giữ kho trước auth |
| UX-02 | Hai người mua vé cuối | Một hold thành công; người còn lại thấy dữ liệu stock mới |
| UX-03 | Reload checkout | Timer lấy expiresAt từ server; không reset hold |
| UX-04 | Đồng hồ về 0 khi payment đang pending | Không tự kết luận thất bại; query trạng thái authoritative |
| UX-05 | Return URL báo success nhưng server pending | Hiển thị pending, không có vé giả |
| UX-06 | Callback trùng; refresh result nhiều lần | Không issue thêm vé, trạng thái nhất quán |
| UX-07 | Success đến sau expiresAt | Hiển thị ngoại lệ; không cấp vé; Admin A-10 xử lý thủ công |
| UX-08 | Free ticket | Không gọi gateway; đơn xác nhận và issue đúng số lượng |
| UX-09 | Issuance/Email thất bại | Trạng thái phát hành và lỗi email không biến order thành failed; retry backend |
| UX-10 | Organizer rejected rồi gửi lại | Lý do hiển thị; chỉ action hợp lệ; Admin thấy hồ sơ mới |
| UX-11 | Approve event | Event APPROVED, chưa public đến khi Organizer publish |
| UX-12 | Giảm quota dưới sold + reserved | UI báo lỗi và backend từ chối; không thay stock |
| UX-13 | Hai scanner cùng quét 1 QR | Một SUCCESS, một ALREADY_USED; không double count |
| UX-14 | Camera denied / mất mạng | Có lookup khi online; offline không ghi check-in thành công |
| UX-15 | Scan wrong event/voided | Từ chối đúng lý do được phép, không thay trạng thái vé |
| UX-16 | Hủy sự kiện đã có giao dịch | Dừng bán/scan, void vé theo backend, thông báo và ngoại lệ tài chính |
| UX-17 | Organizer truy cập event của tổ chức khác | Không lộ data; 403/404 |
| UX-18 | Đối soát còn exception | Không thể request/settle; lý do hiển thị |
| UX-19 | Timeout lúc xác nhận chuyển tiền | Check state và ref trước retry, không chuyển hoặc ghi nhận trùng |
| UX-20 | Thay đổi commission | Lịch sử giữ snapshot cũ, audit đầy đủ |
| UX-21 | Switch org/logout | Private state và cached results được reset |
| UX-22 | Zoom 200%, width 320px, keyboard | CTA, form, navigation truy cập được; không mất chức năng |

## 12. Implementation handoff for Codex

### 12.1. Frontend structure (framework-agnostic)

```text
frontend/
  app-shell/                 shared header, nav, guards, route layout
  shared/
    ui/                      design tokens, controls, feedback, table
    models/                  API DTO mappings, status label maps
    i18n/                    vi/en dictionaries, currency/date formatting
    services/                API client, auth/session, error mapping
  features/
    customer/                C-01..C-15
    organizer/               O-01..O-14
    check-in/                S-01..S-06
    admin/                   A-01..A-14
    auth/                    X-01..X-02 + shared customer auth screens
```

Cấu trúc trên là đề xuất không khóa Angular/React/Vue, không bắt Codex tạo repo hay code trước khi System Design được thống nhất. Route names và Screen IDs giữ ổn định; component naming theo framework đã chọn. Không tạo backend endpoints chỉ vì UI cần dữ liệu; ghi vào API contract cần xác nhận. Các thao tác như verify, publish, reserve, pay, scan, settlement phải tích hợp backend thay vì hardcode.

### 12.2. Build sequence

| Wave | Deliverables | Gate |
|---|---|---|
| 1 — Foundation | Design tokens, shell, auth pages, role/context guards, reusable states | AUTH flow và 4 workspace shells |
| 2 — Event platform | Organizer onboarding + moderation, event wizard, ticket inventory, discovery | Event publish + tìm kiếm end-to-end |
| 3 — Purchase | Booking, checkout, gateway return, order, ticket details | Free/paid/expiry/late-payment test |
| 4 — Check-in | Scanner, manual lookup, history/stats, assignment | Concurrent scan test, online failure handling |
| 5 — Operations | Dashboards, commission, payment exceptions, settlements, audit/config, email templates | Financial reconciliation/exception workflow |
| 6 — QA | Accessibility, responsive, security and e2e regression | 22 UX test gates + PRD acceptance |

Admin/Email slices đi cùng từng wave theo nghiệp vụ; không trì hoãn moderation hoặc verification tới wave 5. Dữ liệu mock chỉ dùng ở prototype/test, có nhãn rõ và không dùng production. Thiết kế high-fidelity cần tạo sau khi review low-fidelity wireframe và chính sách mở.

### 12.3. Definition of Done per screen

- Route, role guard, data scope và navigation được triển khai đúng Screen ID.
- Các thành phần, controls, actions, validations và common states ở mục 6/7 hoàn chỉnh; server authority được giữ.
- Responsive mobile/tablet/desktop, i18n VI/EN, accessibility và UI tests có coverage theo nghiệp vụ.
- Không có tính năng out-of-scope; dependency API còn thiếu được ghi rõ, không mock thành công trong production.
- Test acceptance của màn hình, integration và UX regression liên quan được chạy; PRD mapping lưu trong backlog/PR.

## 13. Open decisions & assumptions

Phạm vi 13 module đã được người dùng xác nhận. Thiết kế dưới đây là đề xuất cần review, không tự coi là chính sách kinh doanh đã chốt.

| ID | Decision | Đề xuất / cần xác nhận |
|---|---|---|
| D-01 | Visual identity | Indigo light theme, Be Vietnam Pro; logo và bộ ảnh chưa có |
| D-02 | Platform & stack | Responsive web cho cả 4 phân hệ; frontend framework, deployment và domain routing sẽ chốt ở System Design |
| D-03 | Payment gateway | Một provider; chọn VNPay/MoMo/khác, sandbox contract, callback/query và đối soát phù hợp |
| D-04 | Reservation / retry | 10 phút mặc định; payment attempt retry/cancel eligibility, xử lý race với webhook và expiry theo backend |
| D-05 | Organizer verification | Hồ sơ, file limits, roles trong organization, quyền suspend/restore, re-verification khi đổi payout |
| D-06 | Event modification | Sau approval/sau khi bán vé: trường được chỉnh, trường bắt buộc re-review, policy hủy sự kiện và quyền admin |
| D-07 | Check-in | Cửa sổ check-in, quyền xem PII, mã kết quả, request dedupe và thiết bị/camera support |
| D-08 | Attendee model | Dùng buyer name cho MVP nếu không thu attendee riêng; không bắt buộc thêm form mỗi vé trước khi chốt |
| D-09 | Financial policy | Fee rate/cap, snapshot timing, refund/chargeback adjustments, fee pending, manual return evidence và settlement eligibility |
| D-10 | Legal & support | Điều khoản, quyền riêng tư, kênh support, xử lý hủy event, nghĩa vụ hoàn trả tiền và lưu trữ PII |
| D-11 | Analytics & emails | Date basis, điều chỉnh KPI, cách tính active user; thời gian reminder, template copy và email retry policy |
| D-12 | Auth validation | Password policy, verified email gate, session storage, throttling và token lifetime do Security Design quyết định |
| D-13 | Feature ranking | Featured do Admin quản lý; tiêu chí Popular cần thống nhất (ví dụ số vé xác nhận trong period) |

## 14. References & next steps

- Internal source: `01-product-requirements.md`, Event Ticketing Platform PRD v1.1.0, 19/11/2026.
- Tài liệu này không phải Figma prototype, không phải API/DB spec và chưa xác nhận contract của payment gateway. Wireframes mô tả bố cục, không mô phỏng chức năng đang chạy.
- Sau khi duyệt UI/UX: tạo `02-business-rules.md` (nếu tách), `04-system-design.md`, `05-database-design.md`, `06-api-specification.md`, `07-security-design.md`, backlog và test strategy. Mọi thay đổi ngoài MVP phải qua Change Request.


## Local customer checkout extension (decision 0004)

Owner-approved local customer checkout uses normal payment labels without the
former simulation banner or customer-selected outcomes. The method page offers
QR and card, a server-based QR countdown, card-number validation, asynchronous
confirmation and failure/retry states with the configured reason. There is no
external gateway integration. This is the explicit local exception to prototype
label guidance above; production scope remains unchanged. Visual implementation
extends the existing ETP purchase layout and accessible primary/secondary controls.
